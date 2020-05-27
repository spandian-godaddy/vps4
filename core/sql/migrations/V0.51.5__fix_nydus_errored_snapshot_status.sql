-- For a period of time we were setting snapshot status to 'NEW' when it should have been something unique.
-- This query fixes the incorrect statuses so that future snapshot actions can succeed.

INSERT INTO snapshot_status
    (status, status_id)
VALUES
    ('AGENT_DOWN', 11);

UPDATE snapshot
    SET status = (SELECT status_id FROM snapshot_status WHERE status = 'CANCELLED')
FROM snapshot s
    JOIN snapshot_status ss ON s.status = ss.status_id
    JOIN snapshot_action ssa ON s.id = ssa.snapshot_id
    JOIN action_type act ON ssa.action_type_id = act.type_id
WHERE ss.status = 'NEW'
    AND act.type = 'CREATE_SNAPSHOT'
    AND ssa.response ->> 'message' LIKE '% Refusing to take snapshot.';
