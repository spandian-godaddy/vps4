
CREATE TABLE virtual_machine_spec (
    spec_id           SMALLINT      PRIMARY KEY
    , spec_name       VARCHAR(255)  NOT NULL
    , tier			  INTEGER		NOT NULL UNIQUE
    
    , cpu_core_count  INTEGER       NOT NULL
    , memory_mib      INTEGER       NOT NULL
    , disk_gib        INTEGER       NOT NULL
    , name 			  VARCHAR(255)
    
    , valid_on        TIMESTAMP NOT NULL DEFAULT NOW()
    , valid_until     TIMESTAMP NOT NULL DEFAULT 'infinity'
    
    , UNIQUE (spec_name, valid_until)
);

CREATE TABLE control_panel
(
    control_panel_id INT PRIMARY KEY
    , name TEXT NOT NULL UNIQUE
);

CREATE TABLE os_type
(
    os_type_id INT PRIMARY KEY
    , name TEXT NOT NULL UNIQUE 
);

CREATE TABLE image
(
    image_id           bigserial PRIMARY KEY
    , name             TEXT NOT NULL UNIQUE
    , control_panel_id INTEGER NOT NULL REFERENCES control_panel (control_panel_id)
    , os_type_id       INTEGER NOT NULL REFERENCES os_type (os_type_id)
);

CREATE TABLE orion_request (
    orion_guid         UUID PRIMARY KEY
    , tier             INT NOT NULL
    , managed_level    INT NOT NULL
    , operating_system TEXT NOT NULL
    , control_panel    TEXT NOT NULL
    , create_date      TIMESTAMP NOT NULL DEFAULT NOW()
    , provision_date   TIMESTAMP
    , shopper_id       TEXT NOT NULL
);

CREATE TABLE virtual_machine (
    vm_id               BIGINT   PRIMARY KEY
    , orion_guid        UUID     NOT NULL REFERENCES orion_request(orion_guid)
    , name              TEXT     NOT NULL
    , project_id        BIGINT   NOT NULL REFERENCES project(project_id) 
    , spec_id           SMALLINT NOT NULL REFERENCES virtual_machine_spec(spec_id)
    , managed_level     INTEGER  NOT NULL
    , image_id          BIGINT   NOT NULL REFERENCES image(image_id)
    
    , valid_on          TIMESTAMP NOT NULL DEFAULT NOW()
    , valid_until       TIMESTAMP NOT NULL DEFAULT 'infinity'
);


CREATE INDEX virtual_machine_project_id_idx ON virtual_machine (project_id);
CREATE INDEX virtual_machine_vm_id_idx ON virtual_machine (vm_id);


INSERT INTO control_panel(control_panel_id, name) VALUES (0, 'none');
INSERT INTO control_panel(control_panel_id, name) VALUES (1, 'cpanel');
INSERT INTO control_panel(control_panel_id, name) VALUES (2, 'plesk');

INSERT INTO os_type(os_type_id, name) VALUES (1, 'linux');
INSERT INTO os_type(os_type_id, name) VALUES (2, 'windows');

-- 'no control panel', 'linux'
INSERT INTO image (name, control_panel_id, os_type_id) VALUES ('centos-7', 0, 1);

-- 'no control panel', 'windows'
INSERT INTO image (name, control_panel_id, os_type_id) VALUES ('windows-2012r2', 0, 2);

-- 'cpanel', 'linux'
INSERT INTO image (name, control_panel_id, os_type_id) VALUES ('centos-7-cPanel-11', 1, 1);

-- Virtual Machine Specs
INSERT INTO virtual_machine_spec (spec_id, tier, spec_name, cpu_core_count, memory_mib, disk_gib) 
    VALUES (1, 10, 'r0.tiny', 1, 512, 20);

INSERT INTO virtual_machine_spec (spec_id, tier, spec_name, cpu_core_count, memory_mib, disk_gib) 
    VALUES (2, 20, 'r0.small', 1, 1024, 30);

INSERT INTO virtual_machine_spec (spec_id, tier, spec_name, cpu_core_count, memory_mib, disk_gib) 
    VALUES (3, 30, 'r0.medium', 2, 2048, 40);

INSERT INTO virtual_machine_spec (spec_id, tier, spec_name, cpu_core_count, memory_mib, disk_gib) 
    VALUES (4, 40, 'r0.large', 2, 4096, 60);

INSERT INTO virtual_machine_spec (spec_id, tier, spec_name, cpu_core_count, memory_mib, disk_gib) 
    VALUES (5, 50, 'r0.xlarge', 4, 8192, 80);


CREATE OR REPLACE FUNCTION orion_request_create(p_orion_guid UUID,
    p_operating_system TEXT,
    p_tier INT, 
    p_control_panel TEXT, 
    p_managed_level INT,
    p_shopper_id TEXT)

    RETURNS VOID AS $$
DECLARE
BEGIN
    
    INSERT INTO orion_request (orion_guid, operating_system, tier, control_panel, managed_level, shopper_id)
       VALUES (p_orion_guid, p_operating_system, p_tier, p_control_panel, p_managed_level, p_shopper_id);

END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id BIGINT
    , p_orion_guid UUID 
    , p_name TEXT
    , p_project_id BIGINT
    , p_spec_id INT
    , p_managed_level INT
    , p_image_id BIGINT)

    RETURNS VOID AS $$
DECLARE
BEGIN
    
    INSERT INTO virtual_machine (vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id)
       VALUES (p_vm_id, p_orion_guid, p_name, p_project_id, p_spec_id, p_managed_level, p_image_id);
       
    UPDATE orion_request SET provision_date = NOW() WHERE orion_guid = p_orion_guid;

END;
$$ LANGUAGE plpgsql;
