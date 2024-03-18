ALTER TABLE virtual_machine
    ADD COLUMN current_os VARCHAR(255);

UPDATE virtual_machine
SET current_os = image.name
FROM image
WHERE virtual_machine.image_id = image.image_id;

CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id UUID,
                                                     p_vps4_user_id BIGINT,
                                                     p_sgid_prefix TEXT,
                                                     p_orion_guid UUID,
                                                     p_name TEXT,
                                                     p_tier INT,
                                                     p_managed_level INT,
                                                     p_image_name TEXT,
                                                     p_data_center_id INT)
    RETURNS VOID AS $$

DECLARE
    v_spec_id INT;
    v_image_id INT;
    v_project_id BIGINT;
    v_server_type_id INT;
    v_current_os VARCHAR(255);

BEGIN

    SELECT * FROM create_project(p_orion_guid::text, p_vps4_user_id, p_sgid_prefix) INTO v_project_id;

    SELECT image_id, server_type_id, name from image WHERE hfs_name=p_image_name INTO v_image_id, v_server_type_id, v_current_os;
    IF v_image_id IS NULL THEN
        RAISE 'hfs image % not found. Provision will not continue.', p_image_name;
    END IF;

    SELECT spec_id FROM virtual_machine_spec
    WHERE tier=p_tier AND valid_until='infinity' AND server_type_id = v_server_type_id  INTO v_spec_id;
    IF v_spec_id IS NULL THEN
        RAISE 'tier % not found. Provision will not continue.', p_tier;
    END IF;

    INSERT INTO virtual_machine (vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id, data_center_id, current_os)
    VALUES (p_vm_id, p_orion_guid, p_name, v_project_id, v_spec_id, p_managed_level, v_image_id, p_data_center_id, v_current_os);

END;
$$ LANGUAGE plpgsql;
