# Snapshot Retry Flow
Automatic snapshot failures are handled by rescheduling the snapshot to try again in a configurable amount of time.  
Unlike a regular action that will raise an exception to the orchestration engine to be retried, a DoNotRetryException
is thrown telling the orchestration engine to not retry these actions.

## All Snapshot Failure Logic
When a snapshot action errors, whether it be on calling hfs to start the snapshot action or while waiting for the 
action to complete, the snapshot and snapshot action's statuses are marked as "ERROR".  Then the snapshot that was
marked as "DEPRECATING" is revived and marked as "LIVE" again.  Then the logic to determine if a reschedule is needed 
is started.

## Snapshot Reschedule Logic
If the snapshot that failed was an automatic snapshot, we determine if we should retry it by counting all of the failed 
automatic snapshots since the last successful automatic snapshot if there is one. If the number of failed snapshots is 
greater than or equal to the limit of retries we've configured, then we do not reschedule the snapshot, and only log 
a warning that max retries has been exceeded.  Our assumption here is that the monitoring service will have already 
let us know that these backups are failing.  If the number of failed snapshots is less than the limit of retries, then 
we submit a new job to the scheduler to start a new automatic backup in a configurable number of hours.

## Dealing with failed automatic snapshots
When a new automatic snapshot is submitted to the api, the first thing it does is set the previous snapshots in ERROR 
status to CANCELLED.  It does not, however, change the status of the snapshot action.  This is to help us track how many 
snapshots are passing vs failing. 