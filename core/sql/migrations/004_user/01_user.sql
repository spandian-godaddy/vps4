CREATE TABLE vm_user (
  	name				VARCHAR(255)	NOT NULL,
  	virtual_machine_id 	BIGINT 			NOT NULL	REFERENCES virtual_machine(vm_id),
  	admin_enabled		BOOLEAN			NOT NULL	DEFAULT 'FALSE',
  	
  	PRIMARY KEY(virtual_machine_id, name)
);