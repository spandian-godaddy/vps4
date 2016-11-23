CREATE TABLE vm_user (
  	name				VARCHAR(255)	NOT NULL,
  	vm_id               BIGINT 			NOT NULL	REFERENCES virtual_machine(vm_id),
  	admin_enabled		BOOLEAN			NOT NULL	DEFAULT 'FALSE',
  	
  	PRIMARY KEY(vm_id, name)
);