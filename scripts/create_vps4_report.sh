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
output_file="vps4-metrics-${date_prefix}.csv"

## Add new DBs here, add altname as well
datacenters=("p3" "sg2")
p3_altname="phx3"
sg2_altname="sin2"

create_sql_file()
{
    dc=$1
    pgsql_output_csv="/tmp/vps4-metrics-${dc}-${date_prefix}.csv"

    ## SQL Query is formatted for maintenance and convenience. Formatting will be removed when sql file is created.
    read -r -d '' query <<-EOF
	\\COPY (
		SELECT
			v.orion_guid,
			v.vm_id,
			vu.shopper_id,
			v.hfs_vm_id,
			'VPS4' AS product,
			'GD' AS company,
			s.name AS orion_status,
			CASE
				WHEN v.valid_until='infinity' THEN 'active'
				ELSE 'deleted'
			END AS vm_status,
			substring(f.name from 'T[0-9.]*') AS tier,
			regexp_replace(i.name, '\\s*\\(.*\\)$', '') AS os,
			c.name AS control_panel,
			CASE
				WHEN v.managed_level=2 THEN 'fully managed'
				WHEN c.name != 'myh' THEN 'managed'
				ELSE 'self-managed'
			END AS managed_level,
			'${dc}' AS dc,
			to_char(v.valid_on, 'YYYY-MM') AS created_on,
			to_char(v.valid_until, 'YYYY-MM') AS removed_on,
			DATE_PART('epoch', (a.completed-a.created)::interval(0)) * INTERVAL '1 second' AS time_to_provision
		FROM virtual_machine v
		JOIN virtual_machine_spec f ON v.spec_id = f.spec_id
		JOIN image i ON v.image_id = i.image_id
		JOIN control_panel c ON c.control_panel_id =  i.control_panel_id
		JOIN project prj USING (project_id)
        JOIN vps4_user vu ON prj.vps4_user_id = vu.vps4_user_id
		JOIN account_status s ON v.account_status_id = s.account_status_id
		JOIN vm_action a ON v.vm_id = a.vm_id
		JOIN action_type t ON a.action_type_id = t.type_id
		WHERE t.type='CREATE_VM' AND a.completed IS NOT NULL
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
for dc in ${datacenters[@]}
do
    create_sql_file $dc
    run_remote_sql_over_ssh_tunnel $dc
done

# Combine all dc-specific csv files into one output file
cat ${pgsql_output_csv/-${dc}/*} > $output_file
echo "VPS4 metrics report csv file created: $output_file"

