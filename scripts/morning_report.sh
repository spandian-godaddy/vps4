#!/bin/bash

## Script to create an ssh tunnel to each datacenter's DB server and run a psql query and export data to a CSV
##
## Before using this script run ssh-copy-id command to copy your ssh public key to each DB's authorized keys file:
## Example: ssh-copy-id -i ~/.ssh/id_rsa.pub p3plvps4db01
##
## To Run: ./morning_report <<a file that contains an sql query (without a semicolon at the end)
## NOTE:  this script also looks for the first instance of _dc_ and replaces it with the current dc
##        that the script is being ran against.  It is intended that most script will have _dc_ as
##        one of the selected parameters to differentiate between data centers.  This parameter is optional though.

date_prefix=$(date "+%Y-%m-%d")
ssh_tunnel_local_port=9998
ssh_tunnel_remote_port=5432
pgsql_file="/tmp/vps4-metrics.sql"
add_csv_header_once=true
output_file="vps4-metrics-${date_prefix}.csv"

## Add new DBs here, add altname as well
datacenters=("n3" "sg2")
n3_altname="ams3"
sg2_altname="sin2"

create_sql_file()
{
    dc=$1
    sqlQuery=`sed "s/_dc_/$1/" $2`  #replace _dc_ with the current datacenter for printing purposes
    pgsql_output_csv="/tmp/vps4-metrics-${dc}-${date_prefix}.csv"

    ## SQL Query is formatted for maintenance and convenience. Formatting will be removed when sql file is created.
    read -r -d '' query <<-EOF
	\\COPY (
	    ${sqlQuery}
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
    create_sql_file $dc $1
    run_remote_sql_over_ssh_tunnel $dc
done

# Combine all dc-specific csv files into one output file
#cat ${pgsql_output_csv/-${dc}/*} > $output_file
#echo "VPS4 metrics report csv file created: $output_file"
cat ${pgsql_output_csv/-${dc}/*}

