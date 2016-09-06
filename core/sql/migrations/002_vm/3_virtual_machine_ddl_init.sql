CREATE TABLE virtual_machine (
    vm_id       BIGINT    PRIMARY KEY
    , sgid      BIGINT    REFERENCES service_group(sgid) 
    , spec_id   SMALLINT  REFERENCES virtual_machine_spec(spec_id)
    , name		TEXT
);

CREATE INDEX virtual_machine_sgid_idx ON virtual_machine (sgid);
