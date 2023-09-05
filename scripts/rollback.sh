#!/bin/bash
currentVersion=$1
rollbackVersion=$2
environment=$3

echo "Rolling back from version $currentVersion to version $rollbackVersion in $environment environment";

if [[ $environment == "dev" ]]; then
	vps4_mcp=("p3dlvps4mcp.cloud.phx3.gdg")
elif [[ $environment == "test" ]]; then
	vps4_mcp=("p3tlvps4mcp.cloud.phx3.gdg")
elif [[ $environment == "stage" ]]; then
	vps4_mcp=("p3slvps4mcp.cloud.phx3.gdg")
elif [[ $environment == "prod" ]]; then
	vps4_mcp=("p3plvps4mcp.cloud.phx3.gdg" "n3plvps4mcp.cloud.ams3.gdg" "sg2plvps4mcp.cloud.sin2.gdg" "a2plvps4mcp.cloud.iad2.gdg")
else
  echo "Unrecognized environment: $environment"
fi

for mcp in "${vps4_mcp[@]}";
do
  ssh -i ~/.ssh/hfs hfs@${mcp} "
    zkget /mcp/config/service/vps4-web/latest | sed s/$currentVersion/$rollbackVersion/ | zkput /mcp/config/service/vps4-web/latest; \
    zkget /mcp/config/service/vps4-message-consumer/latest | sed s/$currentVersion/$rollbackVersion/ | zkput /mcp/config/service/vps4-message-consumer/latest; \
    zkget /mcp/config/service/hfs-engine/latest | sed s/$currentVersion/$rollbackVersion/ | zkput /mcp/config/service/hfs-engine/latest; \
    zkget /mcp/config/service/vps4-scheduler/latest | sed s/$currentVersion/$rollbackVersion/ | zkput /mcp/config/service/vps4-scheduler/latest
  "
  curl -u $effort_service_account -X DELETE "https://yum.secureserver.net/api/v0.1/repos/$environment/centos/7/noarch/vps4/remove/vps4-web-$currentVersion"
  curl -u $effort_service_account -X DELETE "https://yum.secureserver.net/api/v0.1/repos/$environment/centos/7/noarch/vps4/remove/vps4-message-consumer-$currentVersion"
  curl -u $effort_service_account -X DELETE "https://yum.secureserver.net/api/v0.1/repos/$environment/centos/7/noarch/vps4/remove/vps4-orchestration-plugin-$currentVersion"
  curl -u $effort_service_account -X DELETE "https://yum.secureserver.net/api/v0.1/repos/$environment/centos/7/noarch/vps4/remove/vps4-scheduler-$currentVersion"
done