DROP FUNCTION virtual_machine_provision(UUID, UUID, TEXT, BIGINT, INT, INT, BIGINT);

CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id UUID,
    p_vps4_user_id BIGINT,
    p_data_center_id INT,
    p_sgid_prefix TEXT,
    p_orion_guid UUID, 
    p_name TEXT,
    p_tier INT,
    p_managed_level INT,
    p_image_name TEXT)

    RETURNS VOID AS $$
    
DECLARE
    credit_to_provision credit;
    v_spec_id INT;
    v_image_id INT;
    v_project_id BIGINT;
    
BEGIN
    
    SELECT * FROM credit INTO credit_to_provision WHERE orion_guid = p_orion_guid FOR UPDATE;
    
    IF credit_to_provision.provision_date IS NOT NULL THEN
        RAISE 'Credit for orion guid % is already provisioned', credit_to_provision.orion_guid;
    END IF;
    
    SELECT * FROM create_project(p_orion_guid::text, p_vps4_user_id, p_data_center_id, p_sgid_prefix) INTO v_project_id;
    
    SELECT spec_id FROM virtual_machine_spec WHERE tier=p_tier INTO v_spec_id;
    IF v_spec_id IS NULL THEN
        RAISE 'tier % not found. Provision will not continue.', p_tier;
    END IF;
    
    SELECT image_id from image WHERE hfs_name=p_image_name INTO v_image_id;
    IF v_image_id IS NULL THEN
        RAISE 'hfs image % not found. Provision will not continue.', p_image_name;
    END IF;
    
    UPDATE credit SET provision_date = now() where orion_guid = p_orion_guid;
        
    INSERT INTO virtual_machine (vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id)
       VALUES (p_vm_id, p_orion_guid, p_name, v_project_id, v_spec_id, p_managed_level, v_image_id); 
    
END;
$$ LANGUAGE plpgsql;