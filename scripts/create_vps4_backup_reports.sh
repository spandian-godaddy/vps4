#!/bin/bash

## Script to create an ssh tunnel to each datacenter's DB server and run a psql query and export data to a CSV
##
## Before using this script run ssh-copy-id command to copy your ssh public key to each DB's authorized keys file:
## Example: ssh-copy-id -i ~/.ssh/id_rsa.pub p3plvps4db01

date_prefix=$(date "+%Y-%m-%d")
ssh_tunnel_local_port=9998
ssh_tunnel_remote_port=5432
pgsql_file="/tmp/vps4-metrics.sql"
add_csv_header_once=true


snapshot_types=("AUTOMATIC" "ON_DEMAND")

## Add new DBs here, add altname as well
datacenters=("p3" "sg2")
p3_altname="phx3"

sg2_altname="sin2"

create_sql_file()
{
    snapshot_type=$2
    pgsql_output_csv="/tmp/vps4-auto-backup-metrics-${dc}-${date_prefix}.csv"

    ## SQL Query is formatted for maintenance and convenience. Formatting will be removed when sql file is created.
    read -r -d '' query <<-EOF
	\\COPY (
		SELECT
		        '${dc}' AS dc,
		        vm.orion_guid,
		        vm.vm_id,
		        vu.shopper_id,
		        CASE
                    WHEN vm.valid_until='infinity' THEN 'active'
                    ELSE 'deleted'
                END AS vm_status,
                substring(f.name from 'T[0-9.]*') AS tier,
                regexp_replace(i.name, '\\s*\\(.*\\)$', '') AS os,
                c.name AS control_panel,
                snapshot_action.created,
                snapshot_action.completed,
                DATE_PART('epoch', (snapshot_action.completed-snapshot_action.created)::interval(0)) * INTERVAL '1 second' AS time_to_snapshot,
                action_status.status AS action_status,
                snapshot_status.status as snapshot_status
        FROM snapshot_action
        JOIN snapshot ON snapshot_action.snapshot_id = snapshot.id
        JOIN virtual_machine vm USING (vm_id)
        JOIN virtual_machine_spec f USING (spec_id)
        JOIN image i USING (image_id)
        JOIN control_panel c USING (control_panel_id)
        JOIN project prj ON vm.project_id = prj.project_id
        JOIN vps4_user vu ON prj.vps4_user_id = vu.vps4_user_id
        JOIN action_status USING (status_id)
        JOIN snapshot_type USING (snapshot_type_id)
        JOIN snapshot_status ON snapshot.status = snapshot_status.status_id
        WHERE snapshot_type = '${snapshot_type}'
	) TO '${pgsql_output_csv}' WITH CSV
	EOF

    ## Only print out the column headers once
    if $add_csv_header_once
    then
        query="$query HEADER"
        add_csv_header_once=false
    fi

    ## Prevent bash from assuming asterisks are file globs
    set -f
    ## Echo query without delimiting quotes to remove spacing and newline formatting
    echo $query > $pgsql_file
    set +f
}

run_remote_sql_over_ssh_tunnel() {
    dc=$1

    dc_altname=$(eval echo \$${dc}_altname)
    pgdb_host="${dc}plvps4db01.cloud.${dc_altname}.gdg"
    pgsql_pass=$(grep db.vps4.password ../core/src/main/resources/com/godaddy/vps4/config/prod_${dc_altname}/config.properties | awk -F'=' '{print $2}')

    ## Create a control socket to use ssh tunnelling to hit the DB and then close it when finished
    ssh -M -S vps4-metrics-ctrl-socket -fnNT -L $ssh_tunnel_local_port:$pgdb_host:$ssh_tunnel_remote_port $pgdb_host
    PGPASSWORD=$pgsql_pass psql -p $ssh_tunnel_local_port -h localhost -U vps4_owner vps4 -f $pgsql_file
    ssh -S vps4-metrics-ctrl-socket -O exit $pgdb_host
}

## MAIN - Script starts here
for type in ${snapshot_types[@]}
do
    output_file="vps4-${type}-backup-metrics-${date_prefix}.csv"
    for dc in ${datacenters[@]}
    do
        create_sql_file $dc $type
        run_remote_sql_over_ssh_tunnel $dc
    done

    # Combine all dc-specific csv files into one output file
    cat ${pgsql_output_csv/-${dc}/*} > $output_file

    echo "VPS4 metrics report csv file created: $output_file"
    add_csv_header_once=true
done
