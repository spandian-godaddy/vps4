UPDATE virtual_machine_spec SET (spec_name, cpu_core_count, memory_mib, disk_gib) = ('ded.hdd.c16.r128.d16000', 16, 128, 8000) WHERE tier = 100;
UPDATE virtual_machine_spec SET (spec_name, cpu_core_count, memory_mib, disk_gib) = ('ded.hdd.c16.r256.d16000', 16, 256, 8000) WHERE tier = 110;
UPDATE virtual_machine_spec SET (spec_name, cpu_core_count, memory_mib, disk_gib) = ('ded.ssd.c16.r128.d2048', 16, 128, 1000) WHERE tier = 160;
UPDATE virtual_machine_spec SET (spec_name, cpu_core_count, memory_mib, disk_gib) = ('ded.ssd.c16.r256.d2048', 16, 256, 1000) WHERE tier = 170;
