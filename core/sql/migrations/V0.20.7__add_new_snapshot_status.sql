UPDATE snapshot_status SET status='LIVE' where status_id = 3;
INSERT INTO snapshot_status(status_id, status) VALUES (6, 'DEPRECATING');
INSERT INTO snapshot_status(status_id, status) VALUES (7, 'DEPRECATED');
