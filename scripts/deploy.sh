#!/bin/bash
#
# deploy.sh {version} {env}
#
# e.g. ./deploy.sh 0.0.1-24 dev
#   => "Deploy version 0.0.1-24 to dev"
#
VPS4_VERSION=$1
VPS4_ENV=$2

# number of seconds to allow an app server to respond
# with the correct deployed version before failing
DEPLOY_TIMEOUT="30"

[[ -z $VPS4_VERSION || -z $VPS4_ENV ]] && { echo "usage: deploy.sh version env"; exit 1; }

set -e

# copy in key for the given environment
cp /opt/vps4/keys/vps4.${VPS4_ENV}.key ../core/src/main/resources/

# decrypt configuration for the target environment
echo "Decrypting configuration"
mvn --file ../pom.xml \
  -Dvps4.env=${VPS4_ENV} \
  --projects core exec:java@decrypt-config 

# remove any encrypted files (so we don't push them to zookeeper)
find ../core/src/main/resources -type f -name '*.enc' -exec rm -f {} \;

# push zookeeper configuration
ZK_HOSTS=$(jq -r ".[\"$VPS4_ENV\"].zookeeper | join(\",\")" nodes.json)

echo "Pushing configuration to ${ZK_HOSTS}"
mvn --file ../pom.xml \
  -Dvps4.env=${VPS4_ENV} \
  -Dhfs.zk.hosts=${ZK_HOSTS} \
   --projects core exec:java@zk-init

# migrate database
echo "Migrating database"
mvn --file ../pom.xml \
  -Dvps4.env=${VPS4_ENV} \
  --projects core initialize flyway:migrate


INSTALL_PACKAGE="sudo yum clean --disablerepo=\"*\" --enablerepo=\"vps4\" all; sudo yum --disablerepo=\"*\" --enablerepo=\"vps4\" install -y"
SSH_AUTH="-n -i /opt/vps4/jenkinsKeys/jenkins -o StrictHostKeyChecking=no"

# upgrade orchestration plugin
jq -r ".[\"$VPS4_ENV\"].orchestration[]" nodes.json | while read server; do


    echo "Upgrading orchestration plugin on ${server} to version ${VPS4_VERSION}"
    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl stop hfs-engine"
    ssh $SSH_AUTH vps4jenkins@${server} "$INSTALL_PACKAGE vps4-orchestration-plugin-${VPS4_VERSION}"
    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl start hfs-engine"
done

# upgrade app servers
jq -r ".[\"$VPS4_ENV\"].app[]" nodes.json | while read server; do

    echo "Upgrading server ${server} to version ${VPS4_VERSION}"
    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl stop vps4-web"
    ssh $SSH_AUTH vps4jenkins@${server} "$INSTALL_PACKAGE vps4-web-${VPS4_VERSION}"
    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl start vps4-web"

    SECONDS=0
    while true
    do
        DEPLOYED_VERSION=$(curl -s http://${server}:8080/ | jq -r '.version?')
        
        if [[ "$DEPLOYED_VERSION" == "$VPS4_VERSION" ]]; then
            echo "Version ${VPS4_VERSION} deployed to ${server} in ${SECONDS} seconds"
            break
        fi

        if (($SECONDS >= $DEPLOY_TIMEOUT)); then
            echo "Version ${VPS4_VERSION} not deployed on ${server} after $SECONDS seconds (found ${DEPLOYED_VERSION} instead)"
            exit 1
        fi

        sleep 1s
    done
done

# upgrade vps4-message-consumer on kafka servers
jq -r ".[\"$VPS4_ENV\"].consumer[]" nodes.json | while read server; do

    echo "Upgrading vps4 message consumer on ${server} to version ${VPS4_VERSION}"
    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl stop vps4-message-consumer"
    ssh $SSH_AUTH vps4jenkins@${server} "$INSTALL_PACKAGE vps4-message-consumer-${VPS4_VERSION}"
    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl start vps4-message-consumer"
done

# upgrade vps4-scheduler on scheduler servers
#jq -r ".[\"$VPS4_ENV\"].scheduler[]" nodes.json | while read server; do
#
#    echo "Upgrading vps4 scheduler on ${server} to version ${VPS4_VERSION}"
#    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl stop vps4-scheduler"
#    ssh $SSH_AUTH vps4jenkins@${server} "$INSTALL_PACKAGE vps4-scheduler-${VPS4_VERSION}"
#    ssh $SSH_AUTH vps4jenkins@${server} "sudo systemctl start vps4-scheduler"
#done
