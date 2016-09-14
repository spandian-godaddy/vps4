DROP FUNCTION IF EXISTS virtual_machine_create(p_orion_guid UUID, 
	os_type_id INT, 
	p_control_panel_id INT, 
	p_spec_name INT, 
	p_managed_level INT);

CREATE OR REPLACE FUNCTION virtual_machine_create(p_orion_guid UUID, 
	os_type_id INT, 
	p_control_panel_id INT, 
	p_spec_id INT, 
	p_managed_level INT)

    RETURNS VOID AS $$
DECLARE
BEGIN
	
	INSERT INTO virtual_machine (orion_guid, project_id, spec_id, control_panel_id, os_type_id)
	   VALUES (p_orion_guid, os_type_id, p_control_panel_id, p_spec_id, p_managed_level);

END;
$$ LANGUAGE plpgsql;