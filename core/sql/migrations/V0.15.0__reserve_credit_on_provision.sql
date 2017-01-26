CREATE OR REPLACE FUNCTION get_and_reserve_vm_credit_for_provision(p_orion_guid UUID)
    
RETURNS credit AS $$
DECLARE 
    credit_to_provision credit;

BEGIN
	
	SELECT * FROM credit INTO credit_to_provision WHERE orion_guid = p_orion_guid FOR UPDATE;
	
	IF credit_to_provision.provision_date IS NULL THEN
	   UPDATE credit SET provision_date = now() where orion_guid = p_orion_guid;
	END IF;
	
	RETURN credit_to_provision;
	
END 
$$ LANGUAGE plpgsql;
    

CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id UUID
    , p_orion_guid UUID 
    , p_name TEXT
    , p_project_id BIGINT
    , p_spec_id INT
    , p_managed_level INT
    , p_image_id BIGINT)

    RETURNS VOID AS $$
BEGIN
    	
    INSERT INTO virtual_machine (vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id)
       VALUES (p_vm_id, p_orion_guid, p_name, p_project_id, p_spec_id, p_managed_level, p_image_id); 
    
END;
$$ LANGUAGE plpgsql;