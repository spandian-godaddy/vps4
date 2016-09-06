
CREATE TABLE virtual_machine_spec (
    spec_id           SMALLINT      PRIMARY KEY
    , spec_name       VARCHAR(255)  NOT NULL
    
    , cpu_core_count  INTEGER       NOT NULL
    , memory_mib      INTEGER       NOT NULL
    , disk_gib        INTEGER       NOT NULL
    , name 			  VARCHAR(255)
    
    , valid_on        TIMESTAMP NOT NULL DEFAULT NOW()
    , valid_until     TIMESTAMP DEFAULT NULL
    
    , UNIQUE (spec_name, valid_until)
);

