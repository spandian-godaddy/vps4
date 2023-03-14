# retry-snapshot-destroys

This script finds any snapshots that failed to be destroyed and automatically retries their destroy action. The auth token is set with the `JWT` env variable, and an optional "WAIT" variable (seconds to wait between destroys) can be used to reduce strain on OpenStack.

Failed destroys are detected with the `GET /appmonitors/pending/backupactions` endpoint.

## Installation insructions

```
cd retry-snapshot-destroys
npm i
```

## Usage instructions
1. Set your Jomax JWT
`export JWT=...`
2. (Optional) Set the number of seconds to sleep between requests
`export WAIT=...`
3. Run the program
`npm run start`
