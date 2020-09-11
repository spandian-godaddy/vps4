##
## Panopta Account Audit
##
## Purpose of this script is to find and remove unexpected (orphaned) panopta
## accounts belonging to VPS4/DED4.
##
## The audit is done by iterating over the full list of active customer
## accounts according to Panopta and comparing the server list of each customer
## to the VPS4 database records. Accounts belong to vps4 are found by looking
## for customers with package "godaddy.hosting".  Due to the multitude of
## changes during Panopta setup it can be tricky linking a panopta record to
## a vps4 record.  Lookups are tried by orion guid if possible or by ip address
## if it can be parsed from the fqdn or additioanl fqdn fields.  Lookup is then
## done on each vps4 database in each data center and if an vm is found then
## the panopta server key in the database is compared to the panopta key.

from datetime import datetime
import json
from pprint import pprint
import random
import re
import requests
from uuid import UUID

requests.packages.urllib3.disable_warnings()

# Starting offset for retreiving panopta customers, vps4 customers tend to start around 3500 (shared with vps3/mt)
start_offset = 3500
## Page size used for server list GETs using hfs pagination
page_size = 50

# Setting clean to true will trigger deletion of customers with no active servers in Panopta
customer_clean = False
# Setting clean to true will trigger deletion of servers in Panopta that are not found in vps4
server_clean = False

panopta_url = 'https://api2.panopta.com'
api_key = 'dfcc9ba3-47e1-44b3-ad2c-19d6d49ca457'

vps4_urls = {'A2' : 'https://vps4.api.iad2.godaddy.com',
             'SG2': 'https://vps4.api.sin2.godaddy.com',
             'N3' : 'https://vps4.api.ams3.godaddy.com',
             'STG': 'https://vps4.api.stg-godaddy.com'}

# JWT needed to lookup servers in VPS4
jwtfile = '/Users/sjohnson/prodjwt'

ip_name_pattern = re.compile('(?:s|ip-)([0-9-]*)(?:\.ip)?\.secureserver\.net')
invalid_ip_ranges = re.compile('(10\.)'
                               '|(172\.1[6-9]\.)|(172\.2[0-9]\.)|(172\.3[0-1]\.)'
                               '|(192\.168\.)'
                               '|(127\.)'
                               '|(169\.254\.)')


class SimplePanoptaClient(object):

    def __init__(self):
        self.url = panopta_url;
        self.headers = {'Authorization': 'ApiKey {}'.format(api_key)}
        self.customer_list_url = '{}/v2/customer'.format(self.url)

    def get_customer_list_url(self):
        query_params = {'limit': page_size, 'offset': start_offset}
        query_params['status'] = 'active'
        req = requests.Request('GET', self.customer_list_url,  params=query_params).prepare()
        return req.url

    def get_customer_list(self):
        query_params = {'limit': page_size, 'offset': start_offset}
        query_params['status'] = 'active'
        resp=requests.get(customer_list_url, headers=self.headers, params=query_params, verify=False)
        resp_json = resp.json()
        return resp_json

    def get_next_page(self, next_url):
        query_params = {'status': 'active'}
        resp = requests.get(next_url, headers=self.headers, params=query_params, verify=False)
        return resp.json()

    def get_server_list(self, partner_customer_key, status):
        server_url = '{}/v2/server'.format(self.url)
        params = {'partner_customer_key': partner_customer_key, 'status': status}
        resp=requests.get(server_url, headers=self.headers, params=params, verify=False)
        return resp.json()

    def cleanup_empty_customer(self, customer_key):
        customer_url = '{}/v2/customer/{}'.format(self.url, customer_key)
        resp=requests.delete(customer_url, headers=self.headers, verify=False)
        print('Removing customer_key: {} : {}'.format(customer_key, resp.status_code))
        return resp.ok

    def cleanup_orphan_server(self, server_id, partner_customer_key):
        server_url = '{}/v2/server/{}'.format(self.url, server_id)
        query_params = {'partner_customer_key': partner_customer_key}
        resp=requests.delete(server_url, headers=self.headers, params=query_params, verify=False)
        print('Removing server_id: {} : {}'.format(server_id, resp.status_code))
        return resp.ok


class SimpleVps4Client(object):

    def __init__(self):
        self.urls = vps4_urls;
        jwt = self.get_jwt()
        self.headers = {'Authorization': 'sso-jwt {}'.format(jwt)}

    def get_jwt(self):
        jwt = None
        with open(jwtfile) as f:
            jwtline = f.read()
            [var,value] = jwtline.split('=')
            jwt = value.strip().strip("'\"")
        return jwt

    def find_vm(self, filter_name, filter_value):
        # Valid filter names: orionGuid, ipAddress
        params = {filter_name: filter_value}
        print('Looking for {} {}'.format(filter_name, filter_value))
        for dc in vps4_urls.keys():
            url = vps4_urls[dc]
            get_url = '{}/api/vms'.format(url)
            print('Searching {}...'.format(url))
            resp = requests.get(get_url, headers=self.headers, params=params, verify=False)
            json = resp.json()
            if len(json) > 0:
                vmId = json[0]['vmId']
                vm_details = self.get_vm_details(url, json[0]['vmId'])
                if vm_details['monitoringAgent'] and 'serverKey' in vm_details['monitoringAgent']:
                    return { 'vm_id': vmId,
                             'dc': dc,
                             'server_key': vm_details['monitoringAgent']['serverKey']}

        return None

    def get_vm_details(self, url, vmid):
        details_url = '{}/api/vms/{}/withDetails'.format(url, vmid)
        print(details_url)
        resp = requests.get(details_url, headers=self.headers, verify=False)
        json = resp.json()
        return json


class AuditInstance(object):
    def __init__(self):
        self.is_orphan = False
        self.is_test = False
        self.dc = None
        self.vm_id =  None
        self.server_id = None
        self.partner_customer_key = None
        self.customer_key = None
        self.server_key = None
        self.name = None
        self.fqdn = None
        self.addtl_fqdns = None
        self.lookup_method = None
        self.lookup_value = None

    def toJson(self):
        return json.dumps(self, default=lambda o: o.__dict__)


def verify_guid(guid):
    try:
        uuid = UUID(guid)
        return True
    except ValueError:
        return False

def get_ip_from_name(name):
    match = re.match(ip_name_pattern, name)
    if match:
        ip = match.group(1).replace('-','.')
        return ip
    return None

def verify_fqdn(fqdn):
    match = re.match('^[0-9\.]+$', fqdn)  # Very crude IP regex, but good enough for this check
    if match is None:
        return False

    if re.match(invalid_ip_ranges, fqdn):  # Exclude private ip ranges
        return False

    return True

def get_ip_from_additional_fqdns(fqdns):
    for fqdn in fqdns:
        if verify_fqdn(fqdn):
            return fqdn
    return None

def determine_vps_lookup_method(server):
    if verify_guid(server['name']):
        return ('name-is-guid', server['name'])

    elif verify_fqdn(server['fqdn']):
        return ('fqdn-is-ip', server['fqdn'])

    ip = get_ip_from_name(server['name'])
    if ip is not None:
        return ('name-is-ip-hostname', ip)

    ip = get_ip_from_additional_fqdns(server['additional_fqdns'])
    if ip is not None:
        return ('additional-ip', ip)

    print('UNKNOWN lookup method')
    return ('unknown', None)

def handle_panopta_server(customer, server):
    vps4_api_filters = {
            'name-is-guid': 'orionGuid',
            'fqdn-is-ip': 'ipAddress',
            'name-is-ip-hostname': 'ipAddress',
            'additional-ip': 'ipAddress' }

    audit_data = AuditInstance()
    audit_data.partner_customer_key = customer['partner_customer_key']
    audit_data.customer_key = customer['customer_key']
    audit_data.server_key = server['server_key']
    url = server['url']
    audit_data.server_id = url[url.rfind('/')+1:]  # last section of url path is server_id
    audit_data.name = server['name']
    audit_data.fqdn = server['fqdn']
    audit_data.addtl_fqdns = server['additional_fqdns']

    if server['device_type'] == 'container':
        print('Skipping... docker container accounts')
        return audit_data

    if customer['partner_customer_key'].startswith('gdtest'):
        audit_data.is_test = True
        return audit_data

    vm_details = None
    audit_data.lookup_method, audit_data.lookup_value = determine_vps_lookup_method(server)
    if audit_data.lookup_method in vps4_api_filters:
        filter_name = vps4_api_filters[audit_data.lookup_method]
        vm_details = vps4_client.find_vm(filter_name, audit_data.lookup_value)
    else:
        print('Maybe ORPHAN - No valid info found : {}'.format(audit_data.toJson()))

    if vm_details and vm_details['server_key'] == audit_data.server_key:
        audit_data.vm_id = vm_details['vm_id']
        audit_data.is_orphan = False
        print('MATCH ({} : {})'.format(vm_details['dc'], vm_details['vm_id']))
    else:
        audit_data.is_orphan = True
        if vm_details:
            audit_data.dc = vm_details['dc']
            print('**ORPHAN - No VPS4 match found in {}'.format(vm_details['dc']))
        else:
            print('**ORPHAN - No VPS4 match')

    if audit_data.is_orphan:
        handle_orphan_server(audit_data.server_id, audit_data.partner_customer_key)

    return audit_data

def handle_orphan_server(server_id, partner_customer_key):
    if server_clean:
        print('Deleting orphaned server {} !!!!!'.format(server_id))
        resp = panopta_client.cleanup_orphan_server(server_id, partner_customer_key)

def handle_empty_customer(customer):
    partner_customer_key = customer['partner_customer_key']
    customer_key = customer['customer_key']
    print('Empty customer - NO SERVERS - {}/{}'.format(partner_customer_key, customer_key))
    if customer_clean:
        print('Deleting empty customer !!!!!')
        resp = panopta_client.cleanup_empty_customer(customer_key)

def handle_panopta_customer(customer):
    # Get active servers
    partner_customer_key = customer['partner_customer_key']
    active_resp = panopta_client.get_server_list(partner_customer_key, 'active');
    server_list = active_resp['server_list']

    # Get suspended servers
    suspend_resp = panopta_client.get_server_list(partner_customer_key, 'suspended');
    server_list.extend(suspend_resp['server_list'])

    server_count = len(server_list)
    if server_count == 0:
        handle_empty_customer(customer)
    else:
        print("Customer {} has {} servers".format(partner_customer_key, server_count))

    servers_data = []
    for server in server_list:
        audit_data = handle_panopta_server(customer, server)
        servers_data.append(audit_data)

    return servers_data

def is_valid_customer(customer):
    if customer['package'] != 'godaddy.hosting':
        print('Skipping... Package: {}'.format(customer['package']))
        return False

    if customer['status'] != 'active':
        print('Skipping... Status: {}'.format(customer['status']))
        return False

    return True


if __name__ == "__main__":

    global panopta_client
    panopta_client = SimplePanoptaClient()

    global vps4_client
    vps4_client = SimpleVps4Client()

    orphan_count = 0
    empty_customer_count = 0
    test_count = 0
    customer_count = 0

    starttime = datetime.now()
    audit_file = 'audit-file-{}'.format(starttime.strftime('%Y-%m-%d-%H-%M'))
    fhandle = open(audit_file, 'w')

    next_url = panopta_client.get_customer_list_url()
    while next_url is not None:
        response = panopta_client.get_next_page(next_url);
        next_url = response['meta']['next']
        offset = response['meta']['offset']
        counter = offset
        print('*** NEXT PAGE *** Processed {} results'.format(offset))
        for customer in response['customer_list']:
            print('--Processing panopta customer : {}'.format(counter))
            counter += 1

            if not is_valid_customer(customer):
                continue

            customer_count += 1
            servers_data = handle_panopta_customer(customer)
            if len(servers_data) == 0:
                empty_customer_count += 1

            for audit_data in servers_data:
                if audit_data.is_orphan:
                    orphan_count += 1
                if audit_data.is_test:
                    test_count += 1
                fhandle.write(audit_data.toJson())
                fhandle.write('\n')
                fhandle.flush()
            print('Customer count : --{}--'.format(customer_count))
            print('Orphan count   : --{}--'.format(orphan_count))
            print('Test Env count : --{}--'.format(test_count))
            print('Empty customers: --{}--'.format(empty_customer_count))

    stoptime = datetime.now()
    print('Started  : {}'.format(starttime))
    print('Completed: {}'.format(stoptime))
    fhandle.close()

