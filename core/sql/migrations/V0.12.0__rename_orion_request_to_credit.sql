ALTER TABLE orion_request RENAME TO credit;

DROP FUNCTION IF EXISTS orion_request_create(UUID, TEXT, INT, TEXT, INT, TEXT);

CREATE OR REPLACE FUNCTION credit_create(p_orion_guid UUID,
    p_operating_system TEXT,
    p_tier INT, 
    p_control_panel TEXT, 
    p_managed_level INT,
    p_shopper_id TEXT)

    RETURNS VOID AS $$
DECLARE
BEGIN
    
    INSERT INTO credit (orion_guid, operating_system, tier, control_panel, managed_level, shopper_id)
       VALUES (p_orion_guid, p_operating_system, p_tier, p_control_panel, p_managed_level, p_shopper_id);

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id UUID
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
       
    UPDATE credit SET provision_date = NOW() WHERE orion_guid = p_orion_guid;

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION auto_create_credit(p_user_id BIGINT, p_tier INT, p_os TEXT, p_control_panel TEXT, p_managed_level INT)
    RETURNS VOID AS $$
DECLARE
    credit_count integer;
    vm_count integer;
    v_shopper_id TEXT;
    v_orion_guid UUID;
BEGIN
    
    SELECT shopper_id INTO v_shopper_id FROM vps4_user WHERE vps4_user_id = p_user_id FOR UPDATE;
    
    SELECT COUNT(1) INTO credit_count FROM credit WHERE shopper_id = v_shopper_id AND provision_date IS NULL;
    
    SELECT COUNT(1) INTO vm_count FROM virtual_machine vm 
       JOIN user_project_privilege up ON up.project_id = vm.project_id 
       JOIN vps4_user u ON up.vps4_user_id = u.vps4_user_id 
       WHERE u.vps4_user_id = p_user_id AND vm.valid_until = 'infinity';
    
       IF credit_count = 0 AND vm_count = 0 THEN
       
        SELECT md5(random()::text || clock_timestamp()::text)::uuid INTO v_orion_guid;
       
        INSERT INTO credit (orion_guid, operating_system, tier, control_panel, managed_level, shopper_id)
            VALUES (v_orion_guid, p_os, p_tier, p_control_panel, p_managed_level, v_shopper_id);
            
       END IF;

END;
$$ LANGUAGE plpgsql;