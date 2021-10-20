#!/bin/bash
# Reads vm ids from file "vmids". It's assumed all the vm ids are from the same dc

# exit when any command fails
set -e

ssoJwt=$(./getSsoJwt.sh)
jwt=$ssoJwt
echo

read -p "Data center (sin2/ams3/iad2):" dc

if [  $dc == "sin2" ]; then
export vps4Api="https://vps4.api.sin2.godaddy.com/api"
elif [  $dc == "ams3" ]; then
export vps4Api="https://vps4.api.ams3.godaddy.com/api"
elif [  $dc == "iad2" ]; then
export vps4Api="https://vps4.api.iad2.godaddy.com/api"
else
echo "Unrecognized dc: $dc"
fi

notify_types=failover/failoverComplete/patching/scheduledMaintenance
read -p "$notify_types:" notify_type
if ! [[ ${notify_types[*]} =~ $notify_type ]]
then
	echo "Unknown messaging endpoint entered: $notify_type"
	exit 1
fi 

if [ $notify_type == "patching" ] || [ $notify_type == "scheduledMaintenance" ]; then
read -p "Patching Start Time (Example: 2007-12-03T10:15:30.00Z):" startTime
read -p "Duration (mins):" duration
fi

counter=1
for vmid in `cat vmids`;
do
echo "$counter: Sending $notify_type message for $vmid"

case "$notify_type" in
failover)
	curl -X POST -H "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" $vps4Api/vms/$vmid/messaging/$notify_type
	;;
failoverComplete)
	curl -X POST -H "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" $vps4Api/vms/$vmid/messaging/$notify_type
	;;
patching)
	curl -X POST -H "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" -d "{\"startTime\":\"$startTime\",\"durationMinutes\":$duration}" $vps4Api/vms/$vmid/messaging/$notify_type
	;;
scheduledMaintenance)
	curl -X POST -H "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" -d "{\"startTime\":\"$startTime\",\"durationMinutes\":$duration}" $vps4Api/vms/$vmid/messaging/$notify_type
	;;
*)
	echo "Unknown messaging endpoint entered: $notify_type"
	;;
esac
((counter++))
done

