-- Disable all ded specs (OVH hardware update)
UPDATE virtual_machine_spec set valid_until=now_utc() where spec_name like 'ded%';

-- Update level-1 and level3 ded specs for new OVH tiers
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, server_type_id, valid_until)
VALUES
(60,  'ded.hdd.c4.r32.d8000', 4, 32768, 8000,  'hdd-1', 1, 2, 'infinity'),
(80,  'ded.hdd.c6.r64.d8000', 8, 65536, 8000,  'hdd-3', 1, 2, 'infinity'),
(120, 'ded.ssd.c4.r32.d1024', 4, 32768, 1024,  'ssd-1', 1, 2, 'infinity'),
(140, 'ded.ssd.c6.r64.d1024', 8, 65536, 1024,  'ssd-3', 1, 2, 'infinity');
