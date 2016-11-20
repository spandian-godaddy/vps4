CREATE TABLE action_type(
	type_id		INTEGER			PRIMARY KEY
	, type		VARCHAR(255)	UNIQUE
);

INSERT INTO action_type(type_id, type) VALUES (1, 'CREATE_VM');
INSERT INTO action_type(type_id, type) VALUES (2, 'RESTART_VM');
INSERT INTO action_type(type_id, type) VALUES (3, 'ENABLE_ADMIN_ACCESS');
INSERT INTO action_type(type_id, type) VALUES (4, 'DISABLE_ADMIN_ACCESS');
INSERT INTO action_type(type_id, type) VALUES (5, 'START_VM');
INSERT INTO action_type(type_id, type) VALUES (6, 'STOP_VM');
INSERT INTO action_type(type_id, type) VALUES (7, 'DESTROY_VM');
INSERT INTO action_type(type_id, type) VALUES (8, 'SET_PASSWORD');