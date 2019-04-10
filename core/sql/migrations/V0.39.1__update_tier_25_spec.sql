-- Deprecate existing spec
UPDATE virtual_machine_spec SET valid_until=NOW() WHERE tier = 25;

-- Create new vm spec
INSERT INTO virtual_machine_spec
(tier, spec_name, cpu_core_count, memory_mib, disk_gib, name, ip_address_count, server_type_id)
VALUES
(25, 'hosting.c2.r8.d60', 2, 8192, 60, '2.2', 1, 1);

-- Vps4 windows plan: 2.2  (spec: hosting.c2.r8.d60) Tier: 25
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=25 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193817, 1193822, 1193825, 1193826, 1193829, 1193832, 1193835, 1193838);

-- Vps4 linux plan: 2.2  (spec: hosting.c2.r8.d60) Tier: 25
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=25 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193673, 1193678, 1193681, 1193682, 1193685, 1193688, 1193691, 1193694);
