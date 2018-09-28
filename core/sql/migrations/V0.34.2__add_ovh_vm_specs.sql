-- Add ded specs for new OVH tiers
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, server_type_id, valid_until)
VALUES
(60,  'ded.hdd.c4.r32.d4000',    4,  32768,  4000,  'hdd-1', 1, 2, 'infinity'),
(70,  'ded.hdd.c8.r32.d4000',    8,  32768,  4000,  'hdd-2', 1, 2, 'infinity'),
(80,  'ded.hdd.c8.r64.d4000',    8,  65536,  4000,  'hdd-3', 1, 2, 'infinity'),
(90,  'ded.hdd.c8.r128.d6000',   8,  131072, 6000,  'hdd-4', 1, 2, 'infinity'),
(100, 'ded.hdd.c16.r128.d12000', 16, 131072, 12000, 'hdd-5', 1, 2, 'infinity'),
(110, 'ded.hdd.c20.r256.d12000', 20, 262144, 12000, 'hdd-6', 1, 2, 'infinity'),
(120, 'ded.ssd.c4.r32.d960',     4,  32768,  960,   'ssd-1', 1, 2, 'infinity'),
(130, 'ded.ssd.c8.r32.d960',     8,  32768,  960,   'ssd-2', 1, 2, 'infinity'),
(140, 'ded.ssd.c8.r64.d960',     8,  65536,  960,   'ssd-3', 1, 2, 'infinity'),
(150, 'ded.ssd.c8.r128.d960',    8,  131072, 960,   'ssd-4', 1, 2, 'infinity'),
(160, 'ded.ssd.c16.r128.d4900',  16, 131072, 4900,  'ssd-5', 1, 2, 'infinity'),
(170, 'ded.ssd.c20.r256.d4900',  20, 262144, 4900,  'ssd-6', 1, 2, 'infinity');
