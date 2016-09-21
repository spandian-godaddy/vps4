DROP FUNCTION IF EXISTS virtual_machine_update(p_orion_guid UUID, p_name TEXT, p_vm_id BIGINT, p_image TEXT, p_data_center_id INT);

CREATE OR REPLACE FUNCTION virtual_machine_update(p_orion_guid UUID, p_name TEXT, p_vm_id BIGINT, p_image TEXT,	p_data_center_id INT)

    RETURNS VOID AS $$
DECLARE
	v_project_id project.project_id%TYPE;
	v_image_id compatible_image.image_id%TYPE;
	
BEGIN
	SELECT project_id INTO v_project_id FROM virtual_machine WHERE orion_guid = p_orion_guid;
	SELECT image_id INTO v_image_id FROM compatible_image WHERE name = p_image;
	
	UPDATE project SET data_center_id = p_data_center_id WHERE project_id = v_project_id;
	UPDATE virtual_machine SET name=p_name, vm_id = p_vm_id, image_id = v_image_id WHERE orion_guid = p_orion_guid;

END;
$$ LANGUAGE plpgsql;