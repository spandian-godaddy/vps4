#!/bin/bash
#
# This file has a bunch of helper functions that can be used to deploy various vps4 components
# 1. DEPLOY_TIMEOUT
# 2. DRYRUN
# 3. VPS4_VERSION
# 4. VPS4_ENVIRONMENT


# copy in key for the given environment
copy_env_private_keys() {
    local VPS4_MAVEN_SUBMODULE="${1:-core}"

    echo -e "\nCopying '${VPS4_MAVEN_SUBMODULE}' private key for environment '${VPS4_ENVIRONMENT}'"
    ${DRYRUN} cp /opt/vps4/keys/vps4.${VPS4_ENVIRONMENT}.key ../${VPS4_MAVEN_SUBMODULE}/src/main/resources/
}

decrypt_config() {
    local VPS4_MAVEN_SUBMODULE="${1:-core}"

    echo -e "\nDecrypting '${VPS4_MAVEN_SUBMODULE}' configuration for environment '${VPS4_ENVIRONMENT}'"
    ${DRYRUN} mvn --file ../pom.xml -Dvps4.env=${VPS4_ENVIRONMENT} --projects ${VPS4_MAVEN_SUBMODULE} exec:java@decrypt-config
}

cleanup_encrypted_config_files() {
    local VPS4_MAVEN_SUBMODULE="${1:-core}"

    echo -e "\nCleaning up encrypted configuration files from '${VPS4_MAVEN_SUBMODULE}'"
    # remove any encrypted files (so we don't push them to zookeeper)
    ${DRYRUN} find ../${VPS4_MAVEN_SUBMODULE}/src/main/resources -type f -name '*.enc' -exec rm -f {} \;
}

get_host_nodes_for_type() {
    # returns a comma separated list of hostname
    local HOST_TYPE="$1"
    local SEPARATOR="${2:-,}"
    echo $(jq -r ".[\"${VPS4_ENVIRONMENT}\"].${HOST_TYPE} | join(\"${SEPARATOR}\")" nodes.json)
}

push_config_to_zookeeper() {
    local VPS4_MAVEN_SUBMODULE="${1:-core}"
    local ZK_HOSTS=$(get_host_nodes_for_type "zookeeper")

    echo -e "\nPushing configuration to ${ZK_HOSTS}"
    ${DRYRUN} mvn --file ../pom.xml \
        -Dvps4.env=${VPS4_ENVIRONMENT} \
        -Dhfs.zk.hosts=${ZK_HOSTS} \
        --projects ${VPS4_MAVEN_SUBMODULE} exec:java@zk-init
}

migrate_database() {
    local VPS4_MAVEN_SUBMODULE="${1:-core}"

    echo -e "\nMigrating '${VPS4_MAVEN_SUBMODULE}' database for environment '${VPS4_ENVIRONMENT}'"
    ${DRYRUN} mvn --file ../pom.xml \
        -Dvps4.env=${VPS4_ENVIRONMENT} \
        --projects ${VPS4_MAVEN_SUBMODULE} initialize flyway:migrate
}

run_command_over_ssh() {
    local SERVER="$1"
    local COMMAND="$2"
    local SSH_AUTH="-n -i /opt/vps4/jenkinsKeys/jenkins -o StrictHostKeyChecking=no"

    ${DRYRUN} ssh ${SSH_AUTH} vps4jenkins@${SERVER} "${COMMAND}"
}

upgrade_package_on_server() {
    local SERVER="$1"
    local PACKAGE_NAME="$2"
    local SYSTEMD_UNIT_NAME="${3:-$PACKAGE_NAME}"

    local INSTALL_PACKAGE="sudo yum clean --disablerepo=\"*\" --enablerepo=\"vps4\" all; sudo yum --disablerepo=\"*\" --enablerepo=\"vps4\" install -y"

    echo -e "\nUpgrading '${PACKAGE_NAME}' on '${SERVER}' to version '${VPS4_VERSION}'"

    run_command_over_ssh ${SERVER} "sudo systemctl is-enabled ${SYSTEMD_UNIT_NAME} > /dev/null 2>&1 \
        && sudo systemctl stop ${SYSTEMD_UNIT_NAME} || echo Service '${SYSTEMD_UNIT_NAME}' not present"
    run_command_over_ssh ${SERVER} "${INSTALL_PACKAGE} ${PACKAGE_NAME}-${VPS4_VERSION}"
    run_command_over_ssh ${SERVER} "sudo systemctl start ${SYSTEMD_UNIT_NAME}"
}

verify_version_upgrade() {
    local SERVER="$1"
    local DEPLOYED_VERSION=

    SECONDS=0
    while true
    do
        DEPLOYED_VERSION=$(curl -s http://${SERVER}:8080/ | jq -r '.version?')

        if [[ "$DEPLOYED_VERSION" == "$VPS4_VERSION" ]]; then
            echo -e "\nVersion '${VPS4_VERSION}' deployed to '${SERVER}' in '${SECONDS}' seconds"
            break
        fi

        if (($SECONDS >= $DEPLOY_TIMEOUT)); then
            echo -e "\nVersion '${VPS4_VERSION}' not deployed on '${SERVER}' after '${SECONDS}' seconds (found '${DEPLOYED_VERSION}' instead)"
            exit 1
        fi

        sleep 1s
    done

}

upgrade_vps4_app() {
    local SERVERS=$(get_host_nodes_for_type "app" " ")

    for SERVER in ${SERVERS}
    do
        upgrade_package_on_server ${SERVER} "vps4-web"
        ${DRYRUN} verify_version_upgrade ${SERVER} "${VPS4_VERSION}"
    done
}

upgrade_orchestration_plugin() {
    local SERVERS=$(get_host_nodes_for_type "orchestration" " ")

    for SERVER in ${SERVERS}
    do
        upgrade_package_on_server ${SERVER} "vps4-orchestration-plugin" "hfs-engine"
    done
}

upgrade_kafka_consumer() {
    local SERVERS=$(get_host_nodes_for_type "consumer" " ")

    for SERVER in ${SERVERS}
    do
        upgrade_package_on_server ${SERVER} "vps4-message-consumer"
    done
}

upgrade_vps4_scheduler() {
    local SERVERS=$(get_host_nodes_for_type "scheduler" " ")

    for SERVER in ${SERVERS}
    do
        upgrade_package_on_server ${SERVER} "vps4-scheduler"
        # TODO: add code to verify the correct version was deployed
    done
}
