import chalk from 'chalk';
import { isIP } from 'net';

/*
    SELECT DISTINCT( vm.vm_id )
    FROM   virtual_machine vm
        JOIN panopta_server ps using(vm_id)
        JOIN panopta_additional_fqdns paf using(server_id)
    WHERE  vm.valid_until > Now_utc();
 */

const api = 'http://localhost:8089/api';
const pckPrefix = 'gdtest_'
const vmIds = [
    '1b045463-735c-419d-811c-9d4d5ee024bb'
];

const panoptaApiKey = process.env['PANOPTA_API_KEY'];
if (!panoptaApiKey) {
    console.log('"PANOPTA_API_KEY" env var not set');
    process.exit();
}

const jwt = process.env.JWT;
if (!jwt) {
    console.log('"JWT" env var not set');
    process.exit();
}

const oldServiceTypeId='31'; // HTTP port check
const newServiceTypeId='51'; // HTTP check

let res;

function isManaged(vm) {
    switch (vm.managedLevel) {
        case 2:
            return true;
        case 1:
            if (vm.image.serverType.platform !== 'OVH')
                return true;
    }
    return false;
}

async function printErr(res, message) {
    console.log(chalk.red.bold(message));
    console.log(chalk.dim(`${res.status} - ${res.statusText}`));
    try {
        console.log(chalk.dim(JSON.stringify(await res.json())));
    } catch(e) {}
}

(async () => {
    for (const vmId of vmIds) {
        console.log(`Processing VM ${vmId}`);

        // Get VM
        res = await fetch(`${api}/vms/${vmId}/withDetails`, {
            headers: { authorization: `sso-jwt ${jwt}` }
        });
        if (!res.ok) {
            await printErr(res, `Failed to get VM ${vmId}`);
            continue;
        }
        const vm = await res.json();

        if (!vm.monitoringAgent) {
            console.log(chalk.dim(`-> Skipping VM ${vmId} since it does not have Panopta`));
            continue;
        }

        // Get VM's old HTTP port checks
        res = await fetch(`https://api2.panopta.com/v2/server/${vm.monitoringAgent.serverId}`
                + '/network_service'
                + `?partner_customer_key=${pckPrefix + vm.shopperId}`
                + '&limit=0', {
            headers: { authorization: `ApiKey ${panoptaApiKey}` }
        });
        if (!res.ok) {
            await printErr(res, `Failed to get network services for VM ${vmId}`);
            continue;
        }
        const oldServices = (await res.json())['network_service_list'].filter(s => {
            const serviceTypeId = s['service_type'].slice(s['service_type'].lastIndexOf('/') + 1);
            return serviceTypeId === oldServiceTypeId;
        });

        // Replace domain monitoring
        for (const service of oldServices) {
            const address = service['server_interface'];

            if (isIP(address)) {
                console.log(chalk.dim(`-> Skipping HTTP port check on IP ${address}`));
                continue;
            }

            console.log(chalk.yellow(`-> Replacing HTTP check on FQDN ${address}`));

            // Remove old HTTP port check
            const serviceId = parseInt(service.url.slice(service.url.lastIndexOf('/') + 1));
            res = await fetch(`https://api2.panopta.com/v2/server/${vm.monitoringAgent.serverId}`
                    + `/network_service/${serviceId}`
                    + `?partner_customer_key=${pckPrefix + vm.shopperId}`, {
                headers: { authorization: `ApiKey ${panoptaApiKey}` },
                method: 'DELETE'
            });
            if (!res.ok) {
                await printErr(res, `Failed to delete HTTP port check ${serviceId} for VM ${vmId}`);
                process.exit();
            }

            // Add new HTTP check
            const body = {
                'service_type': 'https://api2.panopta.com/v2/network_service_type/'
                        + newServiceTypeId,
                'server_interface': address,
                frequency: (isManaged(vm)) ? 60 : 300,
                'exclude_from_availability': true,
                'outage_confirmation_delay': 300,
                port: 80,
                metadata: {
                    'metric_override': false
                }
            };
            res = await fetch(`https://api2.panopta.com/v2/server/${vm.monitoringAgent.serverId}`
                    + '/network_service'
                    + `?partner_customer_key=${pckPrefix + vm.shopperId}`, {
                headers: {
                    authorization: `ApiKey ${panoptaApiKey}`,
                    'content-type': 'application/json'
                },
                method: 'POST',
                body: JSON.stringify(body)
            });
            if (!res.ok) {
                await printErr(res, `Failed to add HTTP check for VM ${vmId}`);
                process.exit();
            }

        }

    }
})();
