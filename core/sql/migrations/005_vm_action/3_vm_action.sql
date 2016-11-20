CREATE TABLE vm_action (
	id						SERIAL		PRIMARY KEY
	, virtual_machine_id 	BIGINT		NOT NULL 	REFERENCES virtual_machine(vm_id)
	, action_type_id		BIGINT		NOT NULL	REFERENCES action_type(type_id) DEFAULT 1
	, vps4_user_id			BIGINT		NOT NULL	REFERENCES vps4_user(vps4_user_id)
	, request				VARCHAR(255)
	, state                 JSON
	, response				JSON
	, status_id				BIGINT		NOT NULL  	REFERENCES action_status(status_id) DEFAULT 1
	, created				TIMESTAMP	NOT NULL	DEFAULT NOW()
	, note					VARCHAR(255)
);