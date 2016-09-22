
CREATE OR REPLACE FUNCTION virtual_machine_create(p_orion_guid UUID,
	p_project_id BIGINT,
	p_image_id INT, 
	p_spec_id INT, 
	p_managed_level INT)

    RETURNS VOID AS $$
DECLARE
BEGIN
	
	INSERT INTO virtual_machine (orion_guid, project_id, image_id, spec_id, managed_level)
	   VALUES (p_orion_guid, p_project_id, p_image_id, p_spec_id, p_managed_level);

END;
$$ LANGUAGE plpgsql;