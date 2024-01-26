import axios from 'axios';
import chalk from 'chalk';
import fs from 'fs';
import https from 'https';

// data

const data = [
    // { vmId: 'c80d528b-5ac0-419c-8e49-19d57f84784e', creditId: '3103df5a-1a2e-4f9a-8bb9-ad7334f2ab73' },
    // { vmId: '00faaeb4-eff6-4c08-a863-9511c71a68ad', creditId: '984df23a-70da-4486-9fa1-68378fe44eee' },
];

// set up environment

const JWT = process.env.JWT;
if (!JWT) {
    console.log(chalk.bold.red('ERROR: JWT environment variable not set.'));
    process.exit();
}

const vps4Api = 'https://vps4.api.iad2.godaddy.com/api';
const hfsApi = 'https://hfs-a2p2.api.iad2.int.godaddy.com/api/v1';

const hfsAgent = new https.Agent({
    rejectUnauthorized: false,
    cert: fs.readFileSync('hfs-certs/a2p2.crt'),
    key: fs.readFileSync('hfs-certs/a2p2.key')
});

const infinity = '+292278994-08-16T23:00:00Z';

// VPS4 API client

async function getActions(vmId, actionType) {
    const res = await axios.get(vps4Api + '/vms/' + vmId + '/actions', {
        headers: { Authorization: 'sso-jwt ' + JWT },
        params: { actionType }
    });
    return res.data;
}

async function getCredit(vmId) {
    const res = await axios.get(vps4Api + '/credits/' + vmId, {
        headers: { Authorization: 'sso-jwt ' + JWT }
    });
    return res.data;
}

async function getVm(vmId) {
    const res = await axios.get(vps4Api + '/vms/' + vmId, {
        headers: { Authorization: 'sso-jwt ' + JWT }
    });
    return res.data;
}

async function destroyVm(vmId) {
    const res = await axios.delete(vps4Api + '/vms/' + vmId, {
        headers: { Authorization: 'sso-jwt ' + JWT }
    });
    return res.data;
}

async function zombieVm(vmId) {
    const res = await axios.post(vps4Api + '/vms/' + vmId + '/zombie', {}, {
        headers: { Authorization: 'sso-jwt ' + JWT }
    });
    return res.data;
}

// HFS API client

async function patchHfsCredit(creditId, vmId) {
    const payload = {
        from: {
            'product_id': null
        },
        to: {
            'product_id': vmId
        }
    };
    const res = await axios.patch(hfsApi + '/ecomm/accounts/' + creditId + '/product_meta',
        payload,
        { httpsAgent: hfsAgent }
    );
    return res.data;
}

// main function

(async () => {
    for (const { vmId, creditId } of data) {
        try {

            console.log(chalk.bold('Checking vmId=' + vmId + ', creditId=' + creditId));

            const vm = await getVm(vmId);
            if (vm.canceled !== infinity) {
                console.log(chalk.dim('-> VM is canceled, skipping'));
                continue;
            }
            console.log(chalk.dim('-> VM is NOT canceled'));

            const credit = await getCredit(creditId);
            if (credit.productId === null) {
                console.log(chalk.dim('--> Product ID is null'));
                const createAction = (await getActions(vmId, 'CREATE_VM')).results?.[0];
                if (!createAction) {
                    console.log(chalk.dim('---> No CREATE_VM action, destroying VM...'));
                    await destroyVm(vmId);
                    continue;
                }
                if (createAction.status === 'ERROR') {
                    console.log(chalk.dim('---> CREATE_VM failed, destroying VM...'));
                    await destroyVm(vmId);
                    continue;
                }
                if (vm.hfsVmId !== 0 || vm.primaryIpAddress !== null) {
                    console.log(chalk.yellow('---> VM instance detected, patching credit...'));
                    await patchHfsCredit(creditId, vmId);
                    console.log(chalk.yellow('---> zombie\'ing VM...'));
                    await zombieVm(vmId);
                    continue;
                }
            }
            console.log(chalk.red('-> SPECIAL CASE, VM ' + vmId + ' REQUIRES MANUAL INVESTIGATION'));

        } catch (err) {
            console.log(chalk.red('FAILED'));
            console.log(chalk.red(err));
            continue;
        }
    }
})();
