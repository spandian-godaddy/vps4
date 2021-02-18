# This one-time use script is intended to backfill panopta_template_id and monitoring plan feature into VPS4 databases
# since such data does not exist in VPS4 databases before VPS4-3013.
#
# Approach:
# * For each of the known six panopta server template Ids, call Panopta API to get a list of servers that have such
#   server template Id applied: GET https://api2.panopta.com/v2/server_template/{server_template_id} endpoint
#
# * For each of the servers returned above, look for its server_id in the panopta_server table across all datacenter DBs.
#   If there is a match, update the entry in panopta_server table with the missing server_template_id. Also for the same
#   vm, insert into the monitoring_pf table with (vm_id, monitoring). Note that the monitoring value is based on such
#   criteria: if the template is the add-on/managed template id then monitoring=1; if the template is the base template
#   id then monitoring=0
#
# IN retrospect, the approach above could be improved. Instead of keeping the DB connecitons open for an extended period
# of time and writing to databases in small increment, I could have generated all SQL statements and saved them in files,
# and wrote to each database with the file input in one batch.


from datetime import datetime
import json
import pandas as pd
import psycopg2
import re
import requests
from sshtunnel import SSHTunnelForwarder
from sqlalchemy import create_engine
from sqlalchemy.exc import SQLAlchemyError
import sys


panopta_server_template_ids = {'managed.linux': 1146058,
                               'managed.windows': 1146093,
                               'addon.linux': 1146060,
                               'addon.windows': 1146096,
                               'base.linux': 1146061,
                               'base.windows': 1146097}
panopta_api_base_url = 'https://api2.panopta.com'
api_key = 'dfcc9ba3-47e1-44b3-ad2c-19d6d49ca457'

db_creds = [
            {'ssh_host': 'vps4-db-n3.int.godaddy.com',
             'psql_user': 'vps4_owner',
             'psql_pass': 'XdLa4(H$qoiH@Y@#"Cg'
            },
            {'ssh_host': 'vps4-db-sg2.int.godaddy.com',
             'psql_user': 'vps4_owner',
             'psql_pass': 'CpJs7{Z"qnH#)#@$Cg'
            },
            {'ssh_host': 'vps4-db-a2.int.godaddy.com',
             'psql_user': 'vps4_owner2',
             'psql_pass': 'h$CWcD?FZ7LS23A%'
            },
            {'ssh_host': 'vps4-db-p3.int.stg-godaddy.com',
             'psql_user': 'vps4_owner',
             'psql_pass': 'vps4_ownerdev'
            },
            {'ssh_host': 'p3tlvps4db01.cloud.phx3.gdg',
            'psql_user': 'vps4_owner',
            'psql_pass': 'vps4_ownerdev'
            }]
pgres_host = 'localhost'
pgres_port = 5432
pgres_db_name = 'vps4'
ssh_user = 'hfs'
ssh_pkey = '~/.ssh/hfs'


class PanoptaClient(object):
    def __init__(self):
        self.url = panopta_api_base_url
        self.headers = {'Authorization': 'ApiKey {}'.format(api_key)}
        self.get_server_template_url = '{}/v2/server_template'.format(self.url)

    def get_server_list_with_template_id(self, server_template_id):
        query_params = {'server_template_id': server_template_id}
        url = '{}/{}'.format(self.get_server_template_url, server_template_id)
        resp=requests.get(url, headers=self.headers, params=query_params, verify=False)
        resp_json = resp.json()
        return resp_json


class PostgresqlConnect(object):
    '''SSH tunneling is required to access VPS4 remote database servers'''
    def __init__(self, pgres_host, pgres_port, psql_user, psql_pass, db, ssh_user, ssh_host, ssh_pkey):
        self.pgres_host = pgres_host
        self.pgres_port = pgres_port
        self.psql_user = psql_user
        self.psql_pass = psql_pass
        self.tunnel = SSHTunnelForwarder(
            (ssh_host, 22),
            ssh_username=ssh_user,
            ssh_private_key=ssh_pkey,
            remote_bind_address=(pgres_host, pgres_port),
        )
        tunnel = self.tunnel
        tunnel.start()
        self.local_port = tunnel.local_bind_port
        print('- Database server {} connected via SSH || Local Port: {}...'.format(ssh_host, self.local_port))
        self.engine = create_engine(f'postgresql://{self.psql_user}:{self.psql_pass}@{self.pgres_host}:{self.local_port}/{db}')

    def backfill_for_server(self, server_id, template_name, template_id):
        connection = self.engine.connect()
        found_match = False
        try:
            sql1 = "UPDATE panopta_server SET template_id = '{}' WHERE server_id = {} AND destroyed = 'infinity' RETURNING vm_id".format(template_id, server_id)
            result = connection.execute(sql1)
            match_row = result.fetchone()
            if match_row is not None:
                found_match = True
                vm_id = match_row['vm_id']
                print('--- Found match for vm_id {} with panopta server_id {}'.format(vm_id, server_id))
                monitoring_value = get_monitoring_value_from_template_name(template_name)
                if monitoring_value is not None:
                    print('---- Update monitoring_pf to {} for vm_id {}'.format(monitoring_value, vm_id))
                    sql2 = "INSERT INTO monitoring_pf (vm_id, monitoring) VALUES ('{}', {}) ON CONFLICT(vm_id) DO NOTHING".format(vm_id, monitoring_value)
                    connection.execute(sql2)
                else:
                    print('---- NOT update monitoring_pf for vm_id {} (not using base/addon template)'.format(vm_id))
        except SQLAlchemyError as e:
            print('*** DB ERROR for handling panopta server_id {} with template {} {}: {}'.format(server_id, template_name, template_id, e))
        return found_match

    def dispose_connection_pool(self):
        self.engine.dispose()


def get_server_id_from_server_entry(server_entry):
    server_entry_pattern = re.compile('https://api2.panopta.com/v2/server/([0-9]+)')
    match = re.match(server_entry_pattern, server_entry)
    if match:
        return match.group(1)
    return None

def get_monitoring_value_from_template_name(template_name):
    x = template_name.split('.')
    if x[0] in ['addon', 'managed']: return True
    elif(x[0] == 'base'): return False
    else: return None

def handle_server_changes_in_db(server_id, template_name, template_id, pgres):
    for p in pgres:
        found_match = p.backfill_for_server(server_id, template_name, template_id)
        if found_match: break


if __name__ == "__main__":

    audit_file = 'panopta_template_id_monitoring_pf_backfill.result'
    sys.stdout = open(audit_file, 'w')
    print('Start backfillig panopta template_id and monitoring plan feature at {}'.format(datetime.now()))

    global panopta_client
    panopta_client = PanoptaClient()

    # instead of recreating new db engine (pool of connections) through SSH tunnel for every query, keep one pool open
    # for each DB
    global pgres
    pgres = []
    for db in db_creds:
        pgres.append(PostgresqlConnect(pgres_host=pgres_host, pgres_port=pgres_port,
                                   psql_user=db['psql_user'],psql_pass=db['psql_pass'], db=pgres_db_name,
                                   ssh_user=ssh_user, ssh_host=db['ssh_host'], ssh_pkey=ssh_pkey))


    for template_name, template_id in panopta_server_template_ids.items():
        response = panopta_client.get_server_list_with_template_id(template_id)
        print("-- Querying Panopta API and found {} servers for template {} {}".format(len(response['applied_servers']), template_name, template_id))
        for server in response['applied_servers']:
            server_id = get_server_id_from_server_entry(server)
            handle_server_changes_in_db(server_id, template_name, template_id, pgres)

    for p in pgres:
        p.dispose_connection_pool()

    print('Backfill is complete at {}'.format(datetime.now()))
    sys.stdout.close()
