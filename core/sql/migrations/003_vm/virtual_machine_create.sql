DROP FUNCTION IF EXISTS virtual_machine_create(orion_guid UUID,
	project_id BIGINT,
	os_type_id INT, 
	control_panel_id INT, 
	spec_name INT, 
	managed_level INT);

CREATE OR REPLACE FUNCTION virtual_machine_create(p_orion_guid UUID,
	p_project_id BIGINT,
	p_os_type_id INT, 
	p_control_panel_id INT, 
	p_spec_id INT, 
	p_managed_level INT)

    RETURNS VOID AS $$
DECLARE
BEGIN
	
	INSERT INTO virtual_machine (orion_guid, project_id, os_type_id, control_panel_id, spec_id, managed_level)
	   VALUES (p_orion_guid, p_project_id, p_os_type_id, p_control_panel_id, p_spec_id, p_managed_level);

END;
$$ LANGUAGE plpgsql;