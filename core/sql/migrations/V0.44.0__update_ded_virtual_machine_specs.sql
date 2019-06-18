DELETE FROM virtual_machine_spec WHERE tier = 60 AND valid_until <> 'infinity';
DELETE FROM virtual_machine_spec WHERE tier = 80 AND valid_until <> 'infinity';
DELETE FROM virtual_machine_spec WHERE tier = 120 AND valid_until <> 'infinity';
DELETE FROM virtual_machine_spec WHERE tier = 140 AND valid_until <> 'infinity';

UPDATE virtual_machine_spec SET cpu_core_count = 4, disk_gib = 4000 WHERE tier = 60;
UPDATE virtual_machine_spec SET cpu_core_count = 6, disk_gib = 4000 WHERE tier = 70;
UPDATE virtual_machine_spec SET cpu_core_count = 6, disk_gib = 4000 WHERE tier = 80;
UPDATE virtual_machine_spec SET cpu_core_count = 8, disk_gib = 8000 WHERE tier = 90;
UPDATE virtual_machine_spec SET cpu_core_count = 16, disk_gib = 8000 WHERE tier = 100;
UPDATE virtual_machine_spec SET cpu_core_count = 16, disk_gib = 8000 WHERE tier = 110;
UPDATE virtual_machine_spec SET cpu_core_count = 4, disk_gib = 500 WHERE tier = 120;
UPDATE virtual_machine_spec SET cpu_core_count = 6, disk_gib = 500 WHERE tier = 130;
UPDATE virtual_machine_spec SET cpu_core_count = 6, disk_gib = 500 WHERE tier = 140;
UPDATE virtual_machine_spec SET cpu_core_count = 8, disk_gib = 1000 WHERE tier = 150;
UPDATE virtual_machine_spec SET cpu_core_count = 16, disk_gib = 1000 WHERE tier = 160;
UPDATE virtual_machine_spec SET cpu_core_count = 16, disk_gib = 1000 WHERE tier = 170;
