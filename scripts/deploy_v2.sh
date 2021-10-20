#!/bin/bash
#
# deploy_v2.sh -v {version} -e {env} -t {timeout}

readonly PROGNAME=$(basename $0)

set -e

source ./deploy_helper.sh

deploy_vps4_app() {
    echo -e "\n\n********** Deploying vps4 api(web) **********"
    copy_env_private_keys
    decrypt_config
    cleanup_encrypted_config_files
    push_config_to_zookeeper
    migrate_database
    upgrade_vps4_app
    echo "****************************************************"
}

deploy_vps4_orchestration_plugin() {
    echo -e "\n\n********** Deploying vps4 orchestration plugin **********"
    upgrade_orchestration_plugin
    echo "****************************************************************"
}

deploy_vps4_kafka_consumer() {
    echo -e "\n\n********** Deploying vps4 kafka consumer **********"
    upgrade_kafka_consumer
    echo "**********************************************************"
}

deploy_vps4_scheduler() {
    echo -e "\n\n********** Deploying vps4 scheduler **********"
    local VPS4_MAVEN_SUBMODULE="vps4-scheduler"

    copy_env_private_keys "${VPS4_MAVEN_SUBMODULE}"
    decrypt_config "${VPS4_MAVEN_SUBMODULE}"
    cleanup_encrypted_config_files "${VPS4_MAVEN_SUBMODULE}"
    push_config_to_zookeeper "${VPS4_MAVEN_SUBMODULE}"
    upgrade_vps4_scheduler
    echo "*****************************************************"
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

    deploy_vps4_app
    deploy_vps4_orchestration_plugin
    deploy_vps4_kafka_consumer
    deploy_vps4_scheduler
}

main "$@"