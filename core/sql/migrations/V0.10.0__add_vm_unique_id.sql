--Add VM ID Field
ALTER TABLE virtual_machine DROP CONSTRAINT virtual_machine_pkey CASCADE;
ALTER TABLE virtual_machine ADD id UUID PRIMARY KEY;


--Add UUID id to create vm function
DROP FUNCTION IF EXISTS virtual_machine_provision(BIGINT, UUID, TEXT, BIGINT, INT, INT, BIGINT);
CREATE OR REPLACE FUNCTION virtual_machine_provision(p_id UUID
	, p_vm_id BIGINT
    , p_orion_guid UUID 
    , p_name TEXT
    , p_project_id BIGINT
    , p_spec_id INT
    , p_managed_level INT
    , p_image_id BIGINT)

    RETURNS VOID AS $$
DECLARE
BEGIN
    
    INSERT INTO virtual_machine (id, vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id)
       VALUES (p_id, p_vm_id, p_orion_guid, p_name, p_project_id, p_spec_id, p_managed_level, p_image_id);
       
    UPDATE orion_request SET provision_date = NOW() WHERE orion_guid = p_orion_guid;

END;
$$ LANGUAGE plpgsql;

--Make ip_address point at virtual_machine.id
ALTER TABLE ip_address DROP COLUMN vm_id;
ALTER TABLE ip_address ADD COLUMN vm_id UUID REFERENCES virtual_machine (id);

DROP FUNCTION IF EXISTS ip_address_create(BIGINT, BIGINT, VARCHAR(15), INT);
CREATE OR REPLACE FUNCTION ip_address_create(
        p_ip_address_id      BIGINT,
        p_vm_id              UUID,
        p_ip_address         VARCHAR(15),
        p_ip_address_type_id INT
    )

    RETURNS VOID AS $$
BEGIN

    -- if this is primary address, change any other primary addresses to secondary
    IF p_ip_address_type_id = 1 THEN
       UPDATE ip_address SET ip_address_type_id = 2 WHERE vm_id = p_vm_id AND ip_address_type_id = 1;
    END IF;
    
    INSERT INTO ip_address (ip_address_id, ip_address, vm_id, ip_address_type_id)
       VALUES (p_ip_address_id, p_ip_address, p_vm_id, p_ip_address_type_id);

END;
$$ LANGUAGE plpgsql;

--Make vm_action point at virtual_machine.id
ALTER TABLE vm_action DROP COLUMN vm_id;
ALTER TABLE vm_action ADD COLUMN vm_id UUID REFERENCES virtual_machine (id);

--Make vm_user point at virtual_machine.id
ALTER TABLE vm_user DROP COLUMN vm_id;
ALTER TABLE vm_user ADD COLUMN vm_id UUID REFERENCES virtual_machine (id);
ALTER TABLE vm_user ADD CONSTRAINT vm_user_pk PRIMARY KEY (vm_id, name);

DROP FUNCTION IF EXISTS user_create(TEXT, BIGINT, BOOLEAN);
CREATE OR REPLACE FUNCTION user_create(p_name TEXT 
    , p_vm_id UUID
    , p_admin_enabled BOOLEAN)

    RETURNS VOID AS $$
DECLARE
BEGIN
    
    INSERT INTO vm_user (vm_id, name, admin_enabled)
       VALUES (p_vm_id, p_name, p_admin_enabled);
       
END;
$$ LANGUAGE plpgsql;
