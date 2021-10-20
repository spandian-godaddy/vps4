#!/bin/bash

# exit when any command fails
set -e

if [ -z ${jwt+y} ]; then
  ssoJwt=$(./getSsoJwt.sh)
  jwt=$ssoJwt
  echo
fi

read -p 'Openstack Zone (sg2p2/n3p2/a2p2):' zone

if [ $zone == sg2p2 ]; then
  zone='SG2P2'
  vps4_api=https://vps4.api.sin2.godaddy.com/api
  hfs_api=https://hfs-sg2p2.api.sin2.int.godaddy.com/api/v1
elif [ $zone == n3p2 ]; then
  zone='N3P2'
  vps4_api=https://vps4.api.ams3.godaddy.com/api
  hfs_api=https://hfs-n3p2.api.ams3.int.godaddy.com/api/v1
elif [ $zone == a2p2 ]; then
  zone='A2P2'
  vps4_api=https://vps4.api.iad2.godaddy.com/api
  hfs_api=https://hfs-a2p2.api.iad2.int.godaddy.com/api/v1
else
  echo Unrecognized zone: $zone
fi

hfs_client_certs_dir=~/source/hfs/Creds/ssl_ca/hfs

cnt=0
# for entry in `head -n 1 goldDiggerResults`;
for nfid in `awk '{print $10}' goldDiggerResults`
do
    resourceId=${nfid##*-}  # retain the part after the last dash
    echo "resource_id: $resourceId"

    # {
        hfsVm=$(curl -sk --cert $hfs_client_certs_dir/$zone/hfs_end_web_developer.crt --key $hfs_client_certs_dir/$zone/hfs_end_web_developer.key -H 'Content-Type: application/json' -H 'Accept: application/json' $hfs_api/vms/$resourceId | jq -r '.sgid, .status, .vmId')
        # echo $hfsVm
        read -r sgid status vmId <<< $(echo $hfsVm)

        if [ $sgid == 'null' ]; then
            nocfoxStatus=$(curl -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" "https://nocfox.api.int.godaddy.com/v1/cloud/servers/$resourceId" | jq -r .status)
            if [ $nocfoxStatus == 'DESTROYED' ]; then
                echo "Needs to be destroyed from OPENSTACK. "
            else
                echo "No HFS record. Checkout Nocfox account"
                curl -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" "https://nocfox.api.int.godaddy.com/v1/cloud/servers/$resourceId"
            fi
        elif [ $sgid == "phase3-tester" ]; then
            echo "Skipping sgid: $sgid"
        elif [ $status != 'DESTROYED' ]; then
            vps4Vm=$(curl -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" "$vps4_api/vms?hfsVmId=$vmId" | jq -r '.[].canceled, .[].vmId')
            read -r canceled vps4VmId <<< $(echo $vps4Vm)

            echo "vps4VmId: $vps4VmId; canceled: $canceled; HFS vmId: $vmId"
            if [ -z "$vps4VmId" ]; then # if empty then destroy
                echo "sgid: $sgid; status: $status; vmId: $vmId"
                echo "Calling HFS to destroy vmId: $vmId"
                curl -X POST -k --cert $hfs_client_certs_dir/$zone/hfs_end_web_developer.crt --key $hfs_client_certs_dir/$zone/hfs_end_web_developer.key -H 'Content-Type: application/json' -H 'Accept: application/json' $hfs_api/vms/$vmId/destroy
                cnt=$((cnt+1))
            else
                echo "Valid VPS4 account!"
            fi
        elif [ $status == 'DESTROYED' ]; then
            echo "vmId: $vmId already DESTROYED"
        fi
done

echo "$cnt have been destroyed in $zone"