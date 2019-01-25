INSERT INTO scheduled_job_type (id, job_group) VALUES (4, 'backups_manual'), (5, 'backups_automatic');

INSERT INTO action_type(type_id, type) VALUES (30, 'SCHEDULE_MANUAL_SNAPSHOT'),
(31, 'RESCHEDULE_AUTO_SNAPSHOT'),
(32, 'RESCHEDULE_MANUAL_SNAPSHOT'),
(33, 'DELETE_MANUAL_SNAPSHOT_SCHEDULE');
