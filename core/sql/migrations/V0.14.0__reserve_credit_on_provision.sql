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
    