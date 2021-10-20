#!/bin/bash
# Queries list of vm ids from db and writes list of ids to a file. Then calls vps4 api to send email notifications. It's assumed all the vm ids are from the same dc.
dcQueryFile=dcQuery.sql

set_env_vars() {
    if [  $dc == sin2 ]; then
        export vps4Api=https://vps4.api.sin2.godaddy.com/api
        export dbHost=sg2plvps4db01.cloud.sin2.gdg
        export dcDir=prod_sin2
    elif [  $dc == ams3 ]; then
        export vps4Api=https://vps4.api.ams3.godaddy.com/api
        export dbHost=n3plvps4db01.cloud.ams3.gdg
        export dcDir=prod_ams3
    elif [  $dc == iad2 ]; then
        export vps4Api=https://vps4.api.iad2.godaddy.com/api
        export dbHost=a2plvps4db01.cloud.iad2.gdg
        export dcDir=prod
    else
        echo Unrecognized dc: $dc
    fi
}

get_config_from_aws() {
  #Get secret for DB credentials

      echo "aws sts assume-role..."
      ASSUMED_ROLE=$(aws sts assume-role --role-arn $AWS_ROLE_ARN --role-session-name accesscredrole 2>/dev/null) || { echo "Error assuming role"; exit 1; }

      AWS_ACCESS_KEY_ID=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"AccessKeyId\"]")
      AWS_SECRET_ACCESS_KEY=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"SecretAccessKey\"]")
      AWS_SESSION_TOKEN=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"SessionToken\"]")

      export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
      export AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}

      echo "aws secretsmanager get-secret-value..."
      aws secretsmanager get-secret-value --secret-id $'/'$dcDir'/config.properties' --query SecretString --output text > config.properties
}

create_vms_file() {

    user=$(grep db.vps4.username config.properties | awk -F'=' '{print $2}')
    pw=$(grep db.vps4.password config.properties | awk -F'=' '{print $2}')
    
    echo "select vm.vm_id from public.virtual_machine vm join virtual_machine_spec spec USING (spec_id) join server_type type USING (server_type_id) where vm.valid_until = 'infinity' and vm.canceled = 'infinity' and type.platform = '$platform';" > $dcQueryFile

    echo "Create vms file for $dc"
    psql "dbname=vps4 host=$dbHost user=$user password=$pw port=5432" -t -f $dcQueryFile > vmids
}

## MAIN - Script starts here
dc=$1
emailType=$2
startTime=$3
duration=$4
dryRun=$5
useFile=$6
platform=$7

unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKE
export AWS_ACCESS_KEY_ID=$8
export AWS_SECRET_ACCESS_KEY=$9
export AWS_ROLE_ARN=${10}

echo "Starting at $(date '+%d/%m/%Y %H:%M:%S')"

set_env_vars
get_config_from_aws

CUSTOM_VMIDS_FILE=custom_vmids
if $useFile -eq 'true'; then
    if ! test -f $CUSTOM_VMIDS_FILE; then
        echo "Exiting script because file not found: $CUSTOM_VMIDS_FILE"
        exit 1
    fi
    vmids_cmd=`cat $CUSTOM_VMIDS_FILE`
else
    create_vms_file
    vmids_cmd=`cat vmids`
fi

echo "Sending $emailType notifications to $dc"

COUNTER=0
for vmid in $vmids_cmd;
do
    if [ $dryRun != 'true' ]; then
        COUNTER=$((COUNTER+1))
    fi
    echo "$COUNTER: Sending $emailType message for $vmid"

    case $emailType in
    patching)
        if [ $dryRun != 'true' ]; then
            curl -X POST -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" -d "{\"startTime\":\"$startTime\",\"durationMinutes\":$duration}" $vps4Api/vms/$vmid/messaging/$emailType
        fi
        ;;
    scheduledMaintenance)
        if [ $dryRun != 'true' ]; then
            curl -X POST -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" -d "{\"startTime\":\"$startTime\",\"durationMinutes\":$duration}" $vps4Api/vms/$vmid/messaging/$emailType
        fi
        ;;
    *)
        echo Unknown messaging endpoint entered: $emailType
        ;;
    esac

    sleep .3 # sleep 300 milliseconds
done

echo "Sent $COUNTER emails"
if test -f $dcQueryFile; then
    unlink $dcQueryFile
fi
