#!/bin/bash
# Assumes that openstack config files are named in the format: <zone>-public

openstack_config_dir=~/bashScripts/openstack
hfs_client_certs_dir=~/source/hfs/Creds/ssl_ca/hfs

ssoJwt=$(./getSsoJwt.sh)
jwt=$ssoJwt
echo

read -p "Openstack Zone (sg2p2/n3p2/a2p2):" zone

if [ $zone == "sg2p2" ]; then
	hfs_api="https://hfs-sg2p2.api.sin2.int.godaddy.com/api/v1"
	vps4_api="https://vps4.api.sin2.godaddy.com/api"
elif [ $zone == "n3p2" ]; then
	hfs_api="https://hfs-n3p2.api.ams3.int.godaddy.com/api/v1"
	vps4_api="https://vps4.api.ams3.godaddy.com/api"
elif [ $zone == "a2p2" ]; then
	hfs_api="https://hfs-a2p2.api.iad2.int.godaddy.com/api/v1"
	vps4_api="https://vps4.api.iad2.godaddy.com/api"
else
	echo "Unrecognized zone: $zone"
fi

source $openstack_config_dir/$zone-public

read -p "Hypervisor:" hypervisor

arr=($(nova hypervisor-servers $hypervisor | tail -n +4 | sed '$d' | awk {'print $2'}))
{
for osid in "${arr[@]}"
do
        curl -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" https://nocfox.api.int.godaddy.com/v1/cloud/servers?openstackGuid=$osid | jq -r .results[].serverId;
done
} |
{
while read nf_id
do
	curl -s -k -E $hfs_client_certs_dir/$zone/hfs_end_web_developer.p12:H0stIngD3v -H "Content-Type: application/json" $hfs_api/vms/$nf_id/ | jq .vmId;
done
} |
while read hfs_id
do
	curl -sH "Authorization: sso-jwt $jwt" -H "Content-Type: application/json" $vps4_api/vms?hfsVmId=$hfs_id | jq -r .[].vmId;
done

