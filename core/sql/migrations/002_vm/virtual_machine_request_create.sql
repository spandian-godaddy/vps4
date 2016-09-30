
CREATE OR REPLACE FUNCTION virtual_machine_request_create(p_orion_guid UUID,
	p_operating_system TEXT,
	p_tier INT, 
	p_control_panel TEXT, 
	p_managed_level INT)

    RETURNS VOID AS $$
DECLARE
BEGIN
	
	INSERT INTO virtual_machine_request (orion_guid, operating_system, tier, control_panel, managed_level)
	   VALUES (p_orion_guid, p_operating_system, p_tier, p_control_panel, p_managed_level);

END;
$$ LANGUAGE plpgsql;