--Add Included IPs 
ALTER TABLE virtual_machine_spec ADD COLUMN ip_address_count INTEGER;

-- Virtual Machine Specs
UPDATE virtual_machine_spec SET (tier, spec_name, name, cpu_core_count, memory_mib, disk_gib, ip_address_count)
    = (10, 'hosting.r1.small.40', 'Power', 1, 2048, 40, 1)
    WHERE spec_id = 1;

UPDATE virtual_machine_spec SET (tier, spec_name, name, cpu_core_count, memory_mib, disk_gib, ip_address_count)
    = (20, 'hosting.r2.small.60', 'Prime', 1, 4096, 60, 1)
    WHERE spec_id = 2;

UPDATE virtual_machine_spec SET (tier, spec_name, name, cpu_core_count, memory_mib, disk_gib, ip_address_count)
    = (30, 'hosting.r1.medium.120', 'Premium', 2, 6144, 120, 2)
    WHERE spec_id = 3;

UPDATE virtual_machine_spec SET (tier, spec_name, name, cpu_core_count, memory_mib, disk_gib, ip_address_count) 
    = (40, 'hosting.r1.large.240', 'Enterprise', 3,  8192, 240, 2)
    WHERE spec_id = 4;
    
UPDATE virtual_machine_spec SET (tier, spec_name, name, cpu_core_count, memory_mib, disk_gib, ip_address_count) 
    = (50, 'depracated', 'depracated', 0,  0, 0, 0)
    WHERE spec_id = 5;
    
ALTER TABLE virtual_machine_spec ALTER COLUMN ip_address_count SET NOT NULL;

--Try to delete this spec if there are no VMs associated with it.
DELETE FROM virtual_machine_spec WHERE spec_id = 5;