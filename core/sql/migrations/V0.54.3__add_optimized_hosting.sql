-- Add new server type for Optimized Hosting
INSERT INTO server_type(server_type_id, server_type, platform) VALUES (3, 'VIRTUAL', 'OPTIMIZED_HOSTING');

-- Update spec constraint to allow duplicate active tiers for separate server types
DROP INDEX IF EXISTS unique_active_tier;
CREATE UNIQUE INDEX unique_active_tier_by_server_type
    ON virtual_machine_spec (tier, server_type_id)
    WHERE valid_until='infinity';

-- Add specs for new Optimized Hosting.  Specs match existing Openstack VPS.
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, server_type_id)
VALUES
(5,  'oh.hosting.c1.r1.d20',     1,  1024,  20,  '1.3',            1, 3),
(10, 'oh.hosting.c1.r2.d40',     1,  2048,  40,  'Launch (T1.1)',  1, 3),
(15, 'oh.hosting.c1.r4.d40',     1,  4096,  40,  '1.2',            1, 3),
(20, 'oh.hosting.c2.r4.d100',    2,  4096,  100, 'Enhance (T2.1)', 1, 3),
(25, 'oh.hosting.c2.r8.d100',    2,  8192,  100, '2.2',            1, 3),
(30, 'oh.hosting.c3.r6.d150',    3,  6144,  150, 'Grow (T3.1)',    1, 3),
(40, 'oh.hosting.c4.r8.d200',    4,  8192,  200, 'Expand (T4.1)',  1, 3),
(41, 'oh.hosting.c4.r16.d200',   4,  16384, 200, '4.2',            1, 3),
(42, 'oh.hosting.c6.r16.d300',   6,  16384, 300, '6.1',            1, 3),
(43, 'oh.hosting.c6.r24.d300',   6,  24576, 300, '6.2',            1, 3),
(44, 'oh.hosting.c8.r16.d400',   8,  16384, 400, '8.1',            1, 3),
(45, 'oh.hosting.c8.r32.d400',   8,  32768, 400, '8.2',            1, 3),
(46, 'oh.hosting.c16.r32.d800',  16, 32768, 800, '16.1',           1, 3),
(47, 'oh.hosting.c16.r64.d800',  16, 65536, 800, '16.2',           1, 3);

-- Add ready images for Optimized Hosting.
INSERT INTO image
  (hfs_name, control_panel_id, os_type_id, name, server_type_id)
VALUES
  ('hfs-centos70-x86_64-vmtempl', 0, 1, 'CentOS 7', 3),
  ('hfs-centos70-cpanel-11-x86_64-vmtempl', 1, 1, 'CentOS 7 (cPanel)', 3);

-- Update sproc to lookup spec with same server_type_id as the provided image
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
        v_server_type_id INT;

    BEGIN

        SELECT * FROM create_project(p_orion_guid::text, p_vps4_user_id, p_sgid_prefix) INTO v_project_id;

        SELECT image_id, server_type_id from image WHERE hfs_name=p_image_name INTO v_image_id, v_server_type_id;
        IF v_image_id IS NULL THEN
            RAISE 'hfs image % not found. Provision will not continue.', p_image_name;
        END IF;

        SELECT spec_id FROM virtual_machine_spec
          WHERE tier=p_tier AND valid_until='infinity' AND server_type_id = v_server_type_id  INTO v_spec_id;
        IF v_spec_id IS NULL THEN
            RAISE 'tier % not found. Provision will not continue.', p_tier;
        END IF;

        INSERT INTO virtual_machine (vm_id, orion_guid, name, project_id, spec_id, managed_level, image_id)
           VALUES (p_vm_id, p_orion_guid, p_name, v_project_id, v_spec_id, p_managed_level, v_image_id);

    END;
$$ LANGUAGE plpgsql;
