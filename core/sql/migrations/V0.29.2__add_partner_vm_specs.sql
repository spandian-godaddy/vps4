-- Create new vm specs for partners HEG and MT
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count)
VALUES
(15, 'hosting.c1.r4.d40', 1, 4096, 40, '1.2', 1),
(25, 'hosting.c2.r8.d60', 2, 8192, 60, '2.2', 1),
(41, 'hosting.c4.r16.d120', 4, 16384, 120, '4.2', 1),
(42, 'hosting.c6.r16.d200', 6, 16384, 200, '6.1', 1),
(43, 'hosting.c6.r24.d200', 6, 24576, 200, '6.2', 1),
(44, 'hosting.c8.r16.d350', 8, 16384, 350, '8.1', 1),
(45, 'hosting.c8.r32.d350', 8, 32768, 350, '8.2', 1),
(46, 'hosting.c16.r32.d600', 16, 32768, 600, '16.1', 1),
(47, 'hosting.c16.r64.d600', 16, 65536, 600, '16.2', 1);

