-- Add new small entry-level vm spec
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, server_type_id, valid_until)
VALUES
(5,  'hosting.c1.r1.d20', 1, 1024, 20,  '1.3', 1, 1, 'infinity');
