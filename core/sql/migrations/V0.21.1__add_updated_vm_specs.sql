-- Auto increment spec_id of virtual_machine_spec
CREATE SEQUENCE virtual_machine_spec_id_seq;
SELECT setval('virtual_machine_spec_id_seq', 4);
ALTER TABLE virtual_machine_spec ALTER COLUMN spec_id SET DEFAULT nextval('virtual_machine_spec_id_seq');

-- Update spec constraint to allow duplicate tiers but only one active
ALTER TABLE virtual_machine_spec DROP CONSTRAINT virtual_machine_spec_tier_key;
CREATE UNIQUE INDEX unique_active_tier ON virtual_machine_spec (tier) WHERE valid_until='infinity';

-- Deprecate existing specs
UPDATE virtual_machine_spec SET valid_until=NOW() WHERE tier IN (10,20,30,40);

-- Create new vm specs reusing same tier levels
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count)
VALUES
(10, 'hosting.c1.r2.d40', 1, 2048, 40, 'Launch (T1.1)', 1),
(20, 'hosting.c2.r4.d60', 2, 4096, 60, 'Enhance (T2.1)', 1),
(30, 'hosting.c3.r6.d90', 3, 6144, 90, 'Grow (T3.1)', 1),
(40, 'hosting.c4.r8.d120', 4, 8192, 120, 'Expand (T4.1)', 1);

-- Update sproc to check for valid spec
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

        SELECT spec_id FROM virtual_machine_spec WHERE tier=p_tier AND valid_until='infinity' INTO v_spec_id;
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
