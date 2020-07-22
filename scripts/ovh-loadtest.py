##
## DED4/OVH Load Test
##
## Purpose of this script is to validate new OVH datacenter hardware and ensure
## DED4 servers can be reliably created and destroyed.
##
## The script uses the HFS API to get inventory, to create, get, and destroy
## new servers.  Uses hfs sgid to track server usage. The script will first
## analyze the current inventory for the hfs ded4 data center by listing the
## available and total servers for each flavor type. The script then prompts to
## run either a "load" or "clean" task. The "load" task will prompt for 
## flavor and image to test and how many to spin up, random is also an available
## option.  The "clean" option will also prompt for how many and then will
## destroy ACTIVE and UNKNOWN servers.
##
## Run in virtual environment with:
## Python 3.7.1
##
## Package    Version
## ---------- ---------
## certifi    2020.6.20
## chardet    3.0.4
## idna       2.10
## pip        10.0.1
## requests   2.24.0
## setuptools 39.0.1
## urllib3    1.25.9
##
## ---
## Example run output:
##
## Tue Jul 21 07:32 PM ~/workspaces/vps4/vps4/scripts (vps4-2665-ovh-loadtest)$ workon ovh-loadtest
## (ovh-loadtest) Tue Jul 21 07:32 PM ~/workspaces/vps4/vps4/scripts (vps4-2665-ovh-loadtest)$ python ovh-loadtest.py
##
## Load test sgid (a2-ded4-loadtest-r3) currently using 154 servers
## Analyzing current server usage: ...
## Current Server Usage :
##   ACTIVE : 149
##   UNKNOWN : 2
##   CREATING : 2
##   NEW : 1
##
## Available OVH inventory:
## ded.hdd.c16.r128.d16000 : (0/14)
## ded.hdd.c16.r256.d16000 : (0/10)
## ded.hdd.c4.r32.d8000 : (0/68)
## ded.hdd.c6.r64.d8000 : (0/16)
## ded.ssd.c16.r128.d2048 : (0/14)
## ded.ssd.c16.r256.d2048 : (2/10)
## ded.ssd.c4.r32.d1024 : (61/62)
## ded.ssd.c6.r64.d1024 : (0/22)
##
## Choose run load test or clean (load, clean) [load]: load
## Choose one of : ded.ssd.c16.r256.d2048, ded.ssd.c4.r32.d1024
## What flavor to use [ded.ssd.c16.r256.d2048]:
## Choose one of : centos7-plesk17_64, win2016-std-new_64, centos7_64, ubuntu1604-server_64, centos7-cpanel-latest_64, random
## What image to use [centos7_64]:
## How many to run in parallel [2]:
##
## Creating 1 of 2 servers: Server# 155 (centos7_64)
## {'actionType': 'CREATE',
##  'completedAt': None,
##  'createdAt': '2020-07-22T02:33:21.777294',
##  'message': None,
##  'numTicks': None,
##  'resultset': None,
##  'state': 'REQUESTED',
##  'tickInfo': None,
##  'tickNum': None,
##  'vmActionId': 176869,
##  'vmId': 49450}
## Creating 2 of 2 servers: Server# 156 (centos7_64)
## {'actionType': 'CREATE',
##  'completedAt': None,
##  'createdAt': '2020-07-22T02:33:22.859772',
##  'message': None,
##  'numTicks': None,
##  'resultset': None,
##  'state': 'REQUESTED',
##  'tickInfo': None,
##  'tickNum': None,
##  'vmActionId': 176870,
##  'vmId': 49451}
## 

import json
from pprint import pprint
import random
import requests

requests.packages.urllib3.disable_warnings()

## Modify this cert path for where the HFS Creds repo is stored
cert_path = '/Users/sjohnson/workspaces/vps4/Creds/ssl_ca/hfs/A2P2/'
cert_file = 'hfs_end_web_developer.crt'
key_file = 'hfs_end_web_developer.key'

## HFS environment to use
hfs_url = 'https://hfs-a2p2.api.iad2.int.godaddy.com'
## Page size used for server list GETs using hfs pagination
page_size = 50
## sgid is used as a way to easily query number and status of created servers
sgid = 'a2-ded4-loadtest-r3'

## Each node is assigned a hostname like a2-loadtest-75.secureserver.net
hostname_prefix = 'a2-loadtest-n'
hostname_domain = '.secureserver.net'

image_types = [
    'centos7-plesk17_64',
    'win2016-std-new_64',
    'centos7_64',
    'ubuntu1604-server_64',
    'centos7-cpanel-latest_64'
];


class SimpleHfsClient(object):

    def __init__(self):
        self.url = hfs_url;
        self.cert = cert_path + cert_file
        self.key = cert_path + key_file 

    def get_inventory(self):
        inventory_url = '{}/api/v1/vms/inventory'.format(self.url)
        resp=requests.get(inventory_url, cert=(self.cert, self.key), verify=False)
        resp_json = resp.json()

        flavors = {}
        for element in resp.json():
            flavors[element['name']] = element
        return flavors;

    def create_server(self, image, flavor, test_number):
        body = {
            'username': 'nocfox',
            'password': 'onevps4ME!',
            'zone': 'ovh-zone-1',
            'hostname': '{}{}{}'.format(hostname_prefix, test_number, hostname_domain),
            'image_name': image,
            'rawFlavor': flavor,
            'sgid': sgid
        }
        create_url = '{}/api/v1/vms/withFlavor'.format(self.url)
        resp = requests.post(create_url, cert=(self.cert, self.key), verify=False, json=body)
        return resp.json()

    def destroy_server(self, server_id):
        destroy_url = '{}/api/v1/vms/{}/destroy'.format(self.url, server_id)
        resp = requests.post(destroy_url, cert=(self.cert, self.key), verify=False)
        return resp.json()

    def get_servers_by_sgid(self, useable_only="false"):
        get_url = '{}/api/v1/vms?sgid={}&useable_only={}&per_page={}'.format(self.url, sgid, useable_only, page_size)
        resp = requests.get(get_url, cert=(self.cert, self.key), verify=False)
        return resp.json()

    def get_next_page(self, relative_next_url):
        next_url = '{}{}'.format(self.url, relative_next_url)
        resp = requests.get(next_url, cert=(self.cert, self.key), verify=False)
        return resp.json()


def prompt_for_user_inputs(flavors, task='load'):
    inputs = {}
    if (task == 'load'):
        print("Choose one of : {}".format(', '.join(flavors.keys())))
        default_flavor = list(flavors.keys())[0]
        inputs['flavor'] = input('What flavor to use [{}]: '.format(default_flavor)) or default_flavor
        if inputs['flavor'] not in flavors.keys():
            print("\nInvalid Flavor! : {} - Must be flavor with available inventory. Exiting...".format(inputs['flavor']))
            exit()

        images = list(image_types)
        images.append('random')
        print("Choose one of : {}".format(', '.join(images)))
        inputs['image'] = input('What image to use [centos7_64]: ') or 'centos7_64'
        if inputs['image'] not in images:
            print("\nInvalid Image! : {} - exiting...".format(inputs['image']))
            exit()

    default_count = flavors.get(inputs['flavor']) if task == 'load' else 20
    inputs['count'] = int(input('How many to run in parallel [{}]: '.format(default_count)) or default_count)
    if task == 'load':
        max_count = flavors.get(inputs['flavor'])
        if inputs['count'] > max_count:
            print("\nOnly {} servers left...".format(max_count))
            inputs['count'] = max_count

    return inputs

def get_available_inventory():
    flavors = hfsClient.get_inventory()
    print('Available OVH inventory:')
    available_flavors = {}
    for key in sorted(flavors):
        flavor = flavors[key]
        total = flavor['available'] + flavor['in_use'] + flavor['reserved']
        print('{} : ({}/{})'.format(flavor['name'], flavor['available'], total))
        if flavor['available'] > 0:
            available_flavors[flavor['name']] = flavor['available']

    print()
    return available_flavors

def get_current_test_number():
    response = hfsClient.get_servers_by_sgid()
    current = int(response['pagination']['total'])
    print("Load test sgid ({}) currently using {} servers".format(sgid, current))
    return current+1

def get_random_image():
    return random.choice(image_types)

def analyze_current_usage():
    print('Analyzing current server usage: ...')
    response = hfsClient.get_servers_by_sgid()
    servers_by_status= {}
    for server in response['results']:
       servers_by_status.setdefault(server['status'], []).append(server)

    next_url = response['pagination']['next']
    while next_url is not None:
        response = hfsClient.get_next_page(next_url)
        next_url = response['pagination']['next']
        for server in response['results']:
           servers_by_status.setdefault(server['status'], []).append(server)

    print('Current Server Usage : ')
    for status in servers_by_status:
        print('  {} : {}'.format(status, len(servers_by_status[status])))
        if status == 'UNKNOWN':
            for server in servers_by_status[status]:
                print('    {} : {} : {}'.format(server['vmId'], server['resource_id'], server['rawFlavor']))

    print()
    return servers_by_status

def check_servers_in_progress():
    pass

def spin_up_new_servers(current_test_number, flavor, image, count):
    for ndx in range(count):
        server_image = get_random_image() if image == 'random' else image
        newserver = hfsClient.create_server(server_image, flavor, current_test_number)
        print("Creating {} of {} servers: Server# {} ({})".format(ndx+1, count, current_test_number, server_image))
        pprint(newserver)
        current_test_number += 1

def do_load_tests(available_inventory, current_test_number, flavor, image, count):
    # Get current available inventory
    # For each flavor spin up all available
    pass
    
def do_clean(servers_by_status, count):
    ## Warning: Servers do not become available again for quite some time
    servers_to_clean = servers_by_status.get('ACTIVE', []) + servers_by_status.get('UNKNOWN', [])

    if len(servers_to_clean) < count:
        count = len(servers_to_clean)
        if count == 0:
            print("No servers found to destroy... doing nothing")
        else:
            print("Only {} servers found to destroy...".format(count))

    print("Destroying servers: ")
    for ndx in range(count):
        server_id = servers_to_clean[ndx]['vmId']
        action = hfsClient.destroy_server(server_id)
        print("Destroying {} of {} servers: Server Id: {}".format(ndx+1, count, server_id))
        pprint(action)

if __name__ == "__main__":
    global hfsClient
    hfsClient = SimpleHfsClient()

    current_test_number = get_current_test_number()
    servers_by_status = analyze_current_usage()
    available_inventory = get_available_inventory()

    task = input('Choose run load test or clean (load, clean) [load]: ') or 'load'
    kwargs=prompt_for_user_inputs(available_inventory, task)

    if task == 'load':
        #do_load_tests(available_inventory, current_test_number, **kwargs)
        spin_up_new_servers(current_test_number, **kwargs)

    elif task == 'clean':
        do_clean(servers_by_status, **kwargs)

