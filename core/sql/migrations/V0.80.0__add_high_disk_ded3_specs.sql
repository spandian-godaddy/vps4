insert into virtual_machine_spec (spec_name, tier, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, server_type_id)
values ('ded3.hosting.c4.r2.d512', 50, 4, 2048, 512, 'Gen4 VPS High Disk 1', 2, 3),
       ('ded3.hosting.c4.r4.d1024', 51, 4, 5096, 1024, 'Gen4 VPS High Disk 2', 3, 3),
       ('ded3.hosting.c4.r8.d1536', 52, 4, 8192, 1536, 'Gen4 VPS High Disk 3', 4, 3),
       ('ded3.hosting.c4.r16.d2048', 53, 4, 16384, 2048, 'Gen4 VPS High Disk 4', 4, 3),
       ('ded3.hosting.c4.r32.d2048', 54, 4, 32768, 2048, 'Gen4 VPS High Disk 5', 4, 3);
