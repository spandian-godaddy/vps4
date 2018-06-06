-- Update vm specs for existing tiers to include oversubscription, mark inactive via valid_until flag until ready
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, valid_until)
VALUES
(20, 'hosting.c2.r4.d100', 2, 4096, 100, 'Enhance (T2.1)', 1, now_utc()),
(30, 'hosting.c3.r6.d150', 3, 6144, 150, 'Grow (T3.1)', 1, now_utc()),
(40, 'hosting.c4.r8.d200', 4, 8192, 200, 'Expand (T4.1)', 1, now_utc());

-- Update partner vm specs to include oversubscription
UPDATE virtual_machine_spec SET spec_name='hosting.c2.r8.d100', disk_gib=100 WHERE tier=25;
UPDATE virtual_machine_spec SET spec_name='hosting.c4.r16.d200', disk_gib=200 WHERE tier=41;
UPDATE virtual_machine_spec SET spec_name='hosting.c6.r16.d300', disk_gib=300 WHERE tier=42;
UPDATE virtual_machine_spec SET spec_name='hosting.c6.r24.d300', disk_gib=300 WHERE tier=43;
UPDATE virtual_machine_spec SET spec_name='hosting.c8.r16.d400', disk_gib=400 WHERE tier=44;
UPDATE virtual_machine_spec SET spec_name='hosting.c8.r32.d400', disk_gib=400 WHERE tier=45;
UPDATE virtual_machine_spec SET spec_name='hosting.c16.r32.d800', disk_gib=800 WHERE tier=46;
UPDATE virtual_machine_spec SET spec_name='hosting.c16.r64.d800', disk_gib=800 WHERE tier=47;
