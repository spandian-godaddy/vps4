CREATE TABLE virtual_machine (
    vm_id        		BIGINT   
    , orion_guid		UUID	 PRIMARY KEY
    , project_id 		BIGINT   REFERENCES project(project_id) 
    , spec_id    		SMALLINT REFERENCES virtual_machine_spec(spec_id)
    , name		 		TEXT
    , control_panel_id 	INTEGER	 REFERENCES control_panel(control_panel_id)
    , os_type_id		INTEGER	 REFERENCES os_type(os_type_id)
    , managed_level		INTEGER	 NOT NULL
    , image_id			INTEGER  REFERENCES compatible_image(image_id)
);

CREATE INDEX virtual_machine_project_id_idx ON virtual_machine (project_id);
CREATE INDEX virtual_machine_vm_id_idx ON virtual_machine (vm_id);