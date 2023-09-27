import axios from 'axios';
import chalk from 'chalk';
import fs from 'node:fs';
import https from 'node:https';
import process from 'node:process';

const source = {
    url: 'https://hfs-a2p2.api.iad2.int.godaddy.com/api',
    agent: new https.Agent({
        rejectUnauthorized: false,
        cert: fs.readFileSync('a2p2.crt'),
        key: fs.readFileSync('a2p2.key')
    }),
}

const destination = {
    url: 'https://hfs.api.phx3.int.godaddy.com/api',
    agent: new https.Agent({
        rejectUnauthorized: false,
        cert: fs.readFileSync('p3.crt'),
        key: fs.readFileSync('p3.key')
    }),
}

const START = process.env.START || 0;
console.log(chalk.dim(`Starting on index ${START} of ips.txt`));

const WAIT = process.env.WAIT || 0;
console.log(chalk.dim(`Using wait time of ${WAIT}s between operations`));

/* HFS API CLIENT */

async function getRelays(api, ip) {
    const res = await axios.get(api.url + `/v1/mailrelay/${ip}`,
                                { httpsAgent: api.agent });
    return res.data;
}

async function setRelays(api, ip, quota, relays) {
    const res = await axios.post(api.url + `/v1/mailrelay/${ip}`,
                                 { quota, relays },
                                 { httpsAgent: api.agent });
    return res.data;
}

/* HELPER FUNCTIONS */

function readFile(filename) {
    return fs
        .readFileSync(filename)
        .toString().trim().split('\n');
}

function wait(sec) {
    return new Promise((resolve) => {
        setTimeout(resolve, sec * 1000);
    });
}

async function processIp(ip) {
    const data = await getRelays(source, ip);
    let resData;
    if (isNaN(data.quota) || data.quota === 5000) {
        resData = await setRelays(destination, ip, 5000, 0);
    } else {
        console.warn(chalk.yellow(`Setting ${ip} to custom quota of ${data.quota}`));
        resData = await setRelays(destination, ip, data.quota, 0);
    }
    if (data.quota !== resData.quota || resData.relays !== 0) {
        throw new Error('Relay data does not match');
    }
};

async function runWithRetries(fn, i) {
    try {
        await fn();
    } catch (e) {
        if (i > 1) {
            console.warn(chalk.yellow('Failed. Retrying...'));
            await runWithRetries(fn, i - 1);
        } else {
            console.error(chalk.red.bold('Can\'t retry forever, giving up'));
            throw e;
        }
    }
}

/* MAIN FUNCTION */

const ips = readFile('ips.txt');
for (let i = START; i < ips.length; i++) {
    await wait(WAIT);
    try {
        console.log(`Processing ${ips[i]}`);
        await runWithRetries(async () => await processIp(ips[i]), 3);
    } catch (e) {
        console.error(chalk.red.bold(`Failed to copy relays for ${ips[i]}, index ${i}`));
        console.error(e);
        break;
    }
}

