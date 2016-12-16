DROP FUNCTION IF EXISTS auto_create_orion_request(BIGINT);

CREATE OR REPLACE FUNCTION auto_create_orion_request(p_user_id BIGINT, p_tier INT, p_os TEXT, p_control_panel TEXT, p_managed_level INT)
    RETURNS VOID AS $$
DECLARE
    credit_count integer;
    vm_count integer;
    v_shopper_id TEXT;
    v_orion_guid UUID;
BEGIN
    
	SELECT shopper_id INTO v_shopper_id FROM vps4_user WHERE vps4_user_id = p_user_id FOR UPDATE;
	
	SELECT COUNT(1) INTO credit_count FROM orion_request WHERE shopper_id = v_shopper_id AND provision_date IS NULL;
	
	SELECT COUNT(1) INTO vm_count FROM virtual_machine vm 
	   JOIN user_project_privilege up ON up.project_id = vm.project_id 
	   JOIN vps4_user u ON up.vps4_user_id = u.vps4_user_id 
       WHERE u.vps4_user_id = p_user_id AND vm.valid_until = 'infinity';
	
       IF credit_count = 0 AND vm_count = 0 THEN
       
        SELECT md5(random()::text || clock_timestamp()::text)::uuid INTO v_orion_guid;
       
        INSERT INTO orion_request (orion_guid, operating_system, tier, control_panel, managed_level, shopper_id)
            VALUES (v_orion_guid, p_os, p_tier, p_control_panel, p_managed_level, v_shopper_id);
            
       END IF;

END;
$$ LANGUAGE plpgsql;