#!/bin/bash
# jwt environment var is required to exist to run this script
# This script reads list of OVH resource names from serverList and writes out the vps4 vmId to ovhServers.

hfs_client_certs_dir=~/source/hfs/Creds/ssl_ca/hfs

if [ -z ${jwt} ]; then
  echo "jwt environment variable NOT SET!"
  exit
fi

# dc=a2p2 # I use this if I don't want to bother being prompted
read -p 'Datacenter (a2p2/n3p2):' dc

if [ $dc == "n3p2" ]; then
	hfs_api="https://hfs-n3p2.api.ams3.int.godaddy.com/api/v1"
	vps4_api="https://vps4.api.ams3.godaddy.com/api"
elif [ $dc == "a2p2" ]; then
	hfs_api="https://hfs-a2p2.api.iad2.int.godaddy.com/api/v1"
	vps4_api="https://vps4.api.iad2.godaddy.com/api"
else
	echo "Unrecognized dc: $dc"
fi

for server in `cat serversList`;
do

echo "$server"
hfsVmId=$(curl -sk --cert $hfs_client_certs_dir/$dc/hfs_end_web_developer.crt --key $hfs_client_certs_dir/$dc/hfs_end_web_developer.key -H 'Content-Type: application/json' -H 'Accept: application/json' $hfs_api/vms/$server | jq -r .vmId)
echo $hfsVmId

vps4VmId=$(curl -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" "$vps4_api/vms?hfsVmId=$hfsVmId" | jq -r .[].vmId)
if [ ! -z "$vps4VmId" -a "$vps4VmId" != " " ]; then
    echo $vps4VmId >> ovhServers
fi

done