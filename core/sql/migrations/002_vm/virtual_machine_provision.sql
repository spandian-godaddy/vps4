CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id BIGINT
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
	   
    UPDATE orion_request SET provision_date = NOW() WHERE orion_guid = p_orion_guid;

END;
$$ LANGUAGE plpgsql;