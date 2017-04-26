-- Add reference to Data Center to credit table
ALTER TABLE credit ADD COLUMN data_center_id INT references data_center (data_center_id);

-- Copy data center from project to credit for each vm
UPDATE credit
SET data_center_id = projVmDcId.data_center_id 
FROM (SELECT data_center_id, orion_guid 
        FROM virtual_machine JOIN project ON virtual_machine.project_id = project.project_id) as projVmDcId
WHERE credit.orion_guid = projVmDcId.orion_guid;

-- Delete the project data center reference
ALTER TABLE project DROP COLUMN data_center_id;

-- Delete create project stored procedure to remove database parameter
DROP FUNCTION IF EXISTS create_project(VARCHAR(255), BIGINT, INT, VARCHAR(255)) ;
        
-- Add new create project stored procedure without database field
CREATE OR REPLACE FUNCTION create_project(p_project_name  VARCHAR(255), 
        p_owner_user_id BIGINT,
        p_sgid_prefix VARCHAR(255))
  RETURNS BIGINT AS $$
  DECLARE
    new_project_id BIGINT;
  BEGIN
    -- create the project
      INSERT INTO project (project_name, vhfs_sgid)
      VALUES (p_project_name, p_sgid_prefix || currval('project_project_id_seq'))
      RETURNING project_id
      INTO new_project_id;

    -- add the owner
    INSERT INTO user_project_privilege (vps4_user_id, project_id, privilege_id) VALUES (p_owner_user_id, new_project_id, 1);

    RETURN new_project_id;

  END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS virtual_machine_provision(UUID, BIGINT, INT, TEXT, UUID, TEXT, INT, INT, TEXT);

CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id UUID,
        p_vps4_user_id BIGINT,
        p_sgid_prefix TEXT,
        p_orion_guid UUID,
        p_name TEXT,
        p_tier INT,
        p_managed_level INT,
        p_image_name TEXT)

        RETURNS VOID AS $$

    DECLARE
        v_spec_id INT;
        v_image_id INT;
        v_project_id BIGINT;

    BEGIN

        SELECT * FROM create_project(p_orion_guid::text, p_vps4_user_id, p_sgid_prefix) INTO v_project_id;

        SELECT spec_id FROM virtual_machine_spec WHERE tier=p_tier INTO v_spec_id;
        IF v_spec_id IS NULL THEN
            RAISE 'tier % not found. Provision will not continue.', p_tier;
        END IF;

        SELECT image_id from image WHERE hfs_name=p_image_name INTO v_image_id;
        IF v_image_id IS NULL THEN
            RAISE 'hfs image % not found. Provision will not continue.', p_image_name;
        END IF;

        INSERT INTO virtual_machine (vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id)
           VALUES (p_vm_id, p_orion_guid, p_name, v_project_id, v_spec_id, p_managed_level, v_image_id);

    END;
$$ LANGUAGE plpgsql;

-- delete  get_project functions because theyre just selects
DROP FUNCTION IF EXISTS get_project(BIGINT);
DROP FUNCTION IF EXISTS get_user_projects_active(BIGINT);
DROP FUNCTION IF EXISTS get_user_projects(BIGINT);
