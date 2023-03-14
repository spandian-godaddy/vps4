const axios = require('axios');
const chalk = require('chalk');

const apis = [
    'https://vps4.api.phx3.godaddy.com/api',
    'https://vps4.api.iad2.godaddy.com/api',
    'https://vps4.api.sin2.godaddy.com/api',
    'https://vps4.api.ams3.godaddy.com/api'
];

const JWT = process.env.JWT;
if (!JWT) {
    console.log('ERROR: JWT environment variable not set');
    process.exit();
}

const WAIT = process.env.WAIT || 0;
console.log(`Using wait time of ${WAIT}s between destroys`);

function wait(sec) {
    return new Promise((resolve) => {
        setTimeout(resolve, sec * 1000);
    });
}

async function processSnapshots(baseUrl) {
    try {
        const res = await axios.get(`${baseUrl}/appmonitors/pending/backupactions`, {
            headers: { Authorization: `sso-jwt ${JWT}`}
        });
        const snapshots = res.data.filter(s => s.actionType === 'DESTROY_SNAPSHOT' && s.actionStatus === 'ERROR');
        const unique = [... new Map(snapshots.map(s => [s.snapshotId, s])).values()];
        console.log(`Found ${snapshots.length} failed snapshot destroy actions across ${unique.length} snapshots.`);
        for (const snapshot of unique) {
            await wait(WAIT);
            try {
                const snapshotDetails = await axios.get(`${baseUrl}/vms/${snapshot.vmId}/snapshots/${snapshot.snapshotId}/withDetails`, {
                    headers: { Authorization: `sso-jwt ${JWT}`}
                })
                const image = snapshotDetails.data.hfsImageId;
                console.log(`Destroying snapshot ${snapshot.snapshotId} image ${image}`);
                await axios.delete(`${baseUrl}/vms/${snapshot.vmId}/snapshots/${snapshot.snapshotId}`, {
                    headers: { Authorization: `sso-jwt ${JWT}`}
                });
            } catch (err) {
                if (err.response) {
                    console.log(`Error occurred for snapshot ${snapshot.snapshotId}: ${err.response.status} ${err.response.statusText}.`);
                } else {
                    console.log(`An unknown error occurred for snapshot ${snapshot.snapshotId}.`);
                }
            }
        };
    } catch (err) {
        console.log('Could not retrieve list of errored destroys. Is your JWT expired?');
    }
};

(async () => {
    for (const api of apis) {
        console.log(chalk.bold(api));
        await processSnapshots(api);
    }
})();
