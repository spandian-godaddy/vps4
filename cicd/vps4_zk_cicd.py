#!/usr/bin/env python
# coding=utf-8

""" Used by CICD job on Jenkins to update Zookeeper with new product stamp version information"""
import getopt
import json
import logging as log
import signal
import sys
import time

from kazoo.client import KazooClient

zk_hosts = {
    'local': 'localhost:2181',
    'dev': 'p3dlvps4zk01.cloud.phx3.gdg:2181',
    'test': 'p3tlvps4zk01.cloud.phx3.gdg:2181',
    'stage': 'p3slvps4zk01.cloud.phx3.gdg:2181',
    'prod_phx3': 'p3plvps4zk01.cloud.phx3.gdg:2181',
    'prod_a2': 'a2plvps4zk01.cloud.iad2.gdg:2181',
    'prod_sin2': 'sg2plvps4zk01.cloud.sin2.gdg:2181',
    'prod_ams3': 'n3plvps4zk01.cloud.ams3.gdg:2181',
    'default': 'localhost:2181',
}

zk_config_node = "/mcp/config/service"
DEPLOYMENT_TIMEOUT = 1500

def main(argv):
    """ product_stamp_zk_cicd.py """
    vps4_mode = ''
    rpm_name = ''
    product_service = ''
    zk_service_node = '/mcp/service'
    verbose = False
    quick_deploy = False
    try:
        opts, args = getopt.getopt(argv, "he:n:p:qz:v", ["env=", "name=", "product=", "zkservicenode=", "verbose", "quickdeploy"])
    except getopt.GetoptError:
        print("Invalid arguments provided")
        print("vps4_zk_cicd.py -e <environment> -n <rpm name> -p <product_service> [-z <zkservicenode>] [-v | --verbose] [-q | --quickdeploy]")
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print("project_stamp_zk_cicd.py -e <environment> -n <rpm name> -p product_service -v")
            sys.exit()
        elif opt in ("-e", "--env"):
            print("Processing '-e' vps4_mode=%s" % arg)
            vps4_mode = arg
        elif opt in ("-n", "--name"):
            print("Processing '-n' rpm name=%s" % arg)
            rpm_name = arg
        elif opt in ("-v", "--verbose"):
            print("Processing '-v'")
            verbose = True
        elif opt in ("-p", "--product"):
            print("Processing '-p' product_service=%s" % arg)
            product_service = arg
        elif opt in ("-z", "--zkservicenode"):
            print("Processing '-z' zk_service_node=%s" % arg)
            zk_service_node = arg
        elif opt in ("-q", "--quickdeploy"):
            print("Processing '-q' quick_deploy enabled")
            quick_deploy = True

    # ENABLE LOGGING LEVEL
    if verbose:
        log.basicConfig(format="%(levelname)s: %(message)s", level=log.DEBUG)
        log.debug("Verbose mode enabled")
    else:
        log.basicConfig(format="%(levelname)s: %(message)s", level=log.INFO)
        log.info("Standard logging")

    if vps4_mode == '' or rpm_name == '' or product_service == '':
        log.error("project_stamp_zk_cicd.py -e <environment> -n <rpm name> -p product_service -v")
        sys.exit()

    log.info("#########################################################")
    log.info("#########################################################")
    log.info("Zookeeper update and MCP deployment starting")
    log.info("#########################################################")
    log.info("#########################################################")
    deployment_timeout()

    zookeeper_nodes = None if vps4_mode == 'local' else zk_hosts[vps4_mode]
    log.info("##############################")
    log.info("Connecting to Zookeeper(s): %s" % zookeeper_nodes)
    log.info("##############################")
    sys.stdout.flush()

    zk = KazooClient(hosts=zookeeper_nodes, read_only=False)
    zk.start()
    poke_zookeeper(zk, rpm_name, product_service, zk_service_node, quick_deploy)
    zk.stop()
    log.info("#########################################################")
    log.info("#########################################################")
    log.info("Zookeeper update and MCP deployment complete")
    log.info("#########################################################")
    log.info("#########################################################")
    sys.stdout.flush()


def handler(signum, frame):
    """
    Timeout handler so we don't wait indefinitely for a deployment
    """
    raise Exception("Timeout waiting for deployments %s %s" % (signum, frame))


def deployment_timeout():
    """
    Set up a timeout to wait on deployments to complete
    """
    signal.signal(signal.SIGALRM, handler)
    signal.alarm(DEPLOYMENT_TIMEOUT)
    log.basicConfig()


def is_service_deployed(zk, service_name, zk_service_node):
    """
    Tests to see if the expected version of a service is deployed
    """
    data, stat = zk.get(zk_config_node + "/" + service_name + "/latest")
    jsondata = json.loads(data.decode('utf-8'))
    expected_version = jsondata[u'rpm']

    # When the service (e.g. vps4-web) is deployed, only the expected version
    #    should have child nodes.  If the expected version does not have any
    #    nodes or if alternate versions still have child nodes then the
    #    deployment is not complete
    app_node = zk_service_node + "/" + service_name
    tracked_versions = zk.get_children(app_node)
    for version in tracked_versions:
        active_servers = zk.get_children(app_node + "/" + version)
        if version == expected_version:
            if len(active_servers) == 0:
                log.info("Waiting on %s to deploy" % version)
                return False
        else:
            if len(active_servers) > 0:
                log.info("Old version %s still running" % version)
                return False

    log.info("Successfully deployed %s" % expected_version)
    return True


def get_zk_count(svc_data):
    """
    Retrieves how many of a given product stamp instances should be running based on Zookeeper settings
    """
    svc_json = json.loads(svc_data)
    log.debug("svc_data=%s" % svc_json)
    return svc_json[u'count']


def poke_zookeeper(zk, rpm_name, product_service, zk_service_node, quick_deploy):
    """
    Update Zookeeper(s) with new vertical service version information
    """
    if not (zk.exists(zk_config_node)):
        log.error("MISSING MCP CONFIG NODE")
        sys.exit(1)
    if not (zk.exists(zk_service_node)):
        log.error("MISSING MCP SERVICE NODE")
        sys.exit(1)

    log.info("#########################################################")
    log.info("Setting Product Stamp version information in Zookeeper")
    log.info("#########################################################")
    sys.stdout.flush()
    log.info("Processing Product Stamp %s" % product_service)
    log.info("    Service %s will be deployed at version %s" % (product_service, rpm_name))

    # Get the current rpm name and version from Zookeeper for the service
    # we are processing
    target_zknode = zk_config_node + "/" + product_service + "/latest"
    if not zk.exists(target_zknode):
        log.error("Missing zookeeper node %s" % target_zknode)
        assert False
        sys.exit(1)
    data, stat = zk.get(zk_config_node + "/" + product_service + "/latest")
    log.debug(zk_config_node + "/" + product_service + "/latest data=%s" % data)
    zk_json = json.loads(data.decode("utf-8"))
    zk_rpm = zk_json[u'rpm']
    data, stat = zk.get(zk_config_node + "/" + product_service)
    log.debug("/%s data=%s" % (product_service, data))
    zk_count = get_zk_count(data.decode("utf-8"))

    # If no nodes are configured to be running we don't have
    # to wait for deployment, just update the node with the
    # latest build info
    is_deployed = True if zk_count == 0 else False

    log.info("    " + product_service + " ZK node current version is: %s with count: %s" % (zk_rpm, zk_count))
    if zk_rpm != rpm_name:
        log.info("    " + product_service + " existing node version %s does not equal version to be deployed of %s" % (
            zk_rpm, rpm_name))
        new_node_str = '{"rpm": "' + rpm_name + '"}'
        log.info("    Updated node should be %s" % new_node_str)
        zk.set(zk_config_node + "/" + product_service + "/latest", new_node_str.encode())
        log.info("    **** Zookeeper node updated - waiting on MCP ****")
    else:
        log.info("    " + product_service + " service does not need Zookeeper updated")
        is_deployed = True

    log.info("##########")
    sys.stdout.flush()
    # Updating of nodes in Zookeeper is now complete

    if quick_deploy:
        log.info("#########################################################")
        log.info("Quick Deploy enabled, NOT waiting on MCP deployment")
        log.info("#########################################################")
        return

    # Now we wait for MCP to finish deploying the VMs
    log.info("#########################################################")
    log.info("Waiting on MCP deployment process to complete")
    log.info("#########################################################")
    sys.stdout.flush()

    # get currently deployed services
    deployed_services = zk.get_children(zk_service_node)
    still_deploying = True if is_deployed is False else False
    loop_time = 0
    while still_deploying is True:
        # Give MCP a chance to do things, otherwise we are just filling the log with spam
        time.sleep(10)
        loop_time += 10
        log.info("MCP wait time (secs): %s" % loop_time)
        sys.stdout.flush()
        still_deploying = False

        # for each deployed service, get its children
        for deployed_service in deployed_services:
            is_deployed = is_service_deployed(zk, deployed_service, zk_service_node)
            if is_deployed is False:
                still_deploying = True


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
