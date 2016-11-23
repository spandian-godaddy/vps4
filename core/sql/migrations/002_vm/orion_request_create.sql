
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