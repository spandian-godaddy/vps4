CREATE TABLE virtual_machine_request (
    orion_guid         UUID PRIMARY KEY
    , tier             INT NOT NULL
    , managed_level    INT NOT NULL
    , operating_system TEXT NOT NULL
    , control_panel    TEXT NOT NULL
    , create_date      TIMESTAMP NOT NULL DEFAULT NOW()
    , provision_date   TIMESTAMP
);

CREATE TABLE virtual_machine (
    vm_id        		BIGINT   PRIMARY KEY
    , orion_guid        UUID     NOT NULL REFERENCES virtual_machine_request(orion_guid)
    , name              TEXT     NOT NULL
    , project_id 		BIGINT   NOT NULL REFERENCES project(project_id) 
    , spec_id    		SMALLINT NOT NULL REFERENCES virtual_machine_spec(spec_id)
    , managed_level		INTEGER	 NOT NULL
    , image_id			INTEGER  NOT NULL REFERENCES image(image_id)
    
    , valid_on          TIMESTAMP NOT NULL DEFAULT NOW()
    , valid_until       TIMESTAMP NOT NULL DEFAULT 'infinity'
);


CREATE INDEX virtual_machine_project_id_idx ON virtual_machine (project_id);
CREATE INDEX virtual_machine_vm_id_idx ON virtual_machine (vm_id);