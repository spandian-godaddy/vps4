#!/bin/bash

readonly PROGNAME=$(basename $0)

set -e

#
# 1. DEPLOY_TIMEOUT
# 2. DRYRUN
# 3. VPS4_VERSION
# 4. VPS4_ENVIRONMENT

get_host_nodes_for_type() {
    # returns a comma separated list of hostname
    local HOST_TYPE="$1"
    local SEPARATOR="${2:-,}"
    echo $(jq -r ".[\"${VPS4_ENVIRONMENT}\"].${HOST_TYPE} | join(\"${SEPARATOR}\")" ../scripts/nodes.json)
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

pre_deploy_vps4_app() {
    echo -e "\n\n********** Performing pre-deployment steps for vps4-web **********"
    push_config_to_zookeeper
    migrate_database
    echo "******************* Pre-deployment steps complete for vps4-web ****************"
}

pre_deploy_vps4_scheduler() {
    echo -e "\n\n********** Performing pre-deployment steps for vps4-scheduler **********"
    local VPS4_MAVEN_SUBMODULE="vps4-scheduler"
    push_config_to_zookeeper "${VPS4_MAVEN_SUBMODULE}"
    echo "******************* Pre-deployment steps complete for vps4-scheduler *************"
}

usage() {
    cat <<- EOF
usage: $PROGNAME options

Deploys the various vps4 components to an environment defined in the nodes.json file

OPTIONS:
    -e <environment>         The vps4 environment to deploy to.
    -v <version>             The vps4 version to deploy
    -t <timeout>             Timeout value before abandoning a deploy
    -d                       Turn on dry run
    -x                       Debug mode (also turns on set -x)
    -h                       Show help
EOF
}

cmdline() {
    if [ -z "$1" ]; then usage; exit 1; fi

    local timeout=''
    local dryrun=''
    local env=''
    local version=''

    while getopts "hdxe:v:t:" OPTION; do
        case "$OPTION" in
            d)
                dryrun=echo
                ;;
            h)
                usage
                exit 0
                ;;
            x)
                set -x
               ;;
            t)
                timeout=${OPTARG}
                ;;
            e)
                env=${OPTARG}
                ;;
            v)
                version=${OPTARG}
                ;;
            *)
                usage
                exit 1
                ;;
        esac
    done

    # number of seconds to allow an app server to respond
    # with the correct deployed version before failing
    readonly DEPLOY_TIMEOUT="${timeout:-30}"
    readonly DRYRUN="${dryrun:-}"
    readonly VPS4_ENVIRONMENT=${env}
    readonly VPS4_VERSION=${version}
}

main() {
    cmdline "$@"
    [[ -z $VPS4_VERSION || -z $VPS4_ENVIRONMENT ]] && { usage; exit 1; }

    pre_deploy_vps4_app
    pre_deploy_vps4_scheduler
}

main "$@"