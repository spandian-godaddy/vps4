CREATE TABLE virtual_machine (
    vm_id        BIGINT    PRIMARY KEY
    , project_id BIGINT    REFERENCES project(project_id) 
    , spec_id    SMALLINT  REFERENCES virtual_machine_spec(spec_id)
    , name		 TEXT
);

CREATE INDEX virtual_machine_sgid_idx ON virtual_machine (project_id);
