CREATE TABLE action_status(
	status_id	INTEGER			PRIMARY KEY
	, status	VARCHAR(255)
);

INSERT INTO action_status(status_id, status) VALUES (1, 'NEW');
INSERT INTO action_status(status_id, status) VALUES (2, 'IN_PROGRESS');
INSERT INTO action_status(status_id, status) VALUES (3, 'COMPLETE');
INSERT INTO action_status(status_id, status) VALUES (4, 'ERROR');
INSERT INTO action_status(status_id, status) VALUES (5, 'CANCELLED');
INSERT INTO action_status(status_id, status) VALUES (6, 'INVALID')