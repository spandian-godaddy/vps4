#!/bin/bash
# massAbuseSuspend.sh
# Reads vmids from file, calls vps4Api/$vmid/withDetails to get shopperId, then calls the scheduler to schedule a one-time auto-backup. One scheduled every 5 minutes.

# exit when any command fails
set -e

ssoJwt=$(./getSsoJwt.sh)
jwt=$ssoJwt
echo

min=2
targetDateTime=$(date -ju -v+"$min"M +%FT%TZ)
counter=0

for vmid in `cat vmids`;
do

echo "Scheduling new snapshot for vmid: $vmid for $targetDateTime"

{
    curl -s -H "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" -H "Accept: application/json" https://vps4.api.iad2.godaddy.com/api/vms/$vmid/withDetails | jq -r .shopperId
} |
while read shopperId
do

curl -X POST -s -k --cert ~/source/hfs/Creds/ssl_ca/vps4/A2-PROD/vps4_end_web_schedulerclient.crt --key ~/source/hfs/Creds/ssl_ca/vps4/A2-PROD/vps4_end_web_schedulerclient.key -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"vmId": "'"$vmid"'", "when":"'"$targetDateTime"'", "jobType":"ONE_TIME", "repeatIntervalInDays":0, "backupName":"auto-backup", "shopperId": "'"$shopperId"'", "scheduledJobType":"BACKUPS_AUTOMATIC"}' https://vps4-cca.api.iad2.godaddy.com/scheduler/vps4/backups/jobs | jq .

done

min=$((min+5))
((counter++))
targetDateTime=$(date -ju -v+"$min"M +%FT%TZ) # set target date to 5 minutes ahead
done

echo
echo "$counter accounts processed"
