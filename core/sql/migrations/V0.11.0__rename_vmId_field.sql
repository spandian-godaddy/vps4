-- Remove old virtual_machine_provision function
DROP FUNCTION IF EXISTS virtual_machine_provision(UUID, BIGINT, UUID, TEXT, BIGINT, INT, INT, BIGINT);

--Remove old vm_id index
DROP INDEX virtual_machine_vm_id_idx;

--Rename vm_id field
ALTER TABLE virtual_machine RENAME vm_id TO hfs_vm_id;
ALTER TABLE virtual_machine RENAME id TO vm_id;

-- Remove hfs_vm_id not null constraint
ALTER TABLE virtual_machine ALTER COLUMN hfs_vm_id DROP NOT NULL;

--Make new index for hfs_vm_id
CREATE INDEX virtual_machine_hfs_vm_id_idx ON virtual_machine (hfs_vm_id);

-- CREATE vm_id index
CREATE INDEX virtual_machine_vm_id_idx ON virtual_machine (vm_id);

--Make new virtual_machine_provision function
CREATE OR REPLACE FUNCTION virtual_machine_provision(p_vm_id UUID
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