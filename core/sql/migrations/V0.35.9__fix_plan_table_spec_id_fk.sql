-- Vps4 linux plan: Launch (T1.1)  (spec: hosting.c1.r2.d40) Tier: 10
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=10 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1066863, 1066868, 1066871, 1066866, 1066875, 1066878, 1066881, 1066884);

-- Vps4 linux plan: 1.2  (spec: hosting.c1.r4.d40) Tier: 15
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=15 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193671, 1193676, 1193679, 1193674, 1193683, 1193686, 1193689, 1193692);

-- Vps4 linux plan: Enhance (T2.1)  (spec: hosting.c2.r4.d60) Tier: 20
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=20 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1066865, 1066870, 1066873, 1066874, 1066877, 1066880, 1066883, 1066886);

-- Vps4 linux plan: 2.2  (spec: hosting.c2.r8.d100) Tier: 25
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=25 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193673, 1193678, 1193681, 1193682, 1193685, 1193688, 1193691, 1193694);

-- Vps4 linux plan: Grow (T3.1)  (spec: hosting.c3.r6.d150) Tier: 30
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=30 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1066888, 1066891, 1066893, 1066894, 1066896, 1066898, 1066900, 1066902);

-- Vps4 linux plan: Expand (T4.1)  (spec: hosting.c4.r8.d200) Tier: 40
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=40 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1066904, 1066907, 1066909, 1066910, 1066912, 1066914, 1066916, 1066918);

-- Vps4 linux plan: 4.2  (spec: hosting.c4.r16.d200) Tier: 41
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=41 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193696, 1193699, 1193701, 1193702, 1193704, 1193706, 1193708, 1193710);

-- Vps4 linux plan: 6.1  (spec: hosting.c6.r16.d300) Tier: 42
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=42 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193712, 1193715, 1193717, 1193718, 1193720, 1193722, 1193724, 1193726);

-- Vps4 linux plan: 6.2  (spec: hosting.c6.r24.d300) Tier: 43
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=43 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193728, 1193731, 1193733, 1193734, 1193736, 1193738, 1193740, 1193742);

-- Vps4 linux plan: 8.1  (spec: hosting.c8.r16.d400) Tier: 44
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=44 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193744, 1193747, 1193749, 1193750, 1193752, 1193754, 1193756, 1193758);

-- Vps4 linux plan: 8.2  (spec: hosting.c8.r32.d400) Tier: 45
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=45 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193760, 1193763, 1193765, 1193766, 1193768, 1193770, 1193772, 1193774);

-- Vps4 linux plan: 16.1  (spec: hosting.c16.r32.d800) Tier: 46
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=46 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193776, 1193779, 1193781, 1193782, 1193784, 1193786, 1193788, 1193790);

-- Vps4 linux plan: 16.2  (spec: hosting.c16.r64.d800) Tier: 47
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=47 AND o.name='linux' AND s.valid_until='infinity'
AND pfid IN (1193792, 1193795, 1193797, 1193798, 1193800, 1193802, 1193804, 1193806);

-- Vps4 windows plan: Launch (T1.1)  (spec: hosting.c1.r2.d40) Tier: 10
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=10 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1066927, 1066932, 1066935, 1066930, 1066939, 1066942, 1066945, 1066948);

-- Vps4 windows plan: 1.2  (spec: hosting.c1.r4.d40) Tier: 15
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=15 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193815, 1193820, 1193823, 1193818, 1193827, 1193830, 1193833, 1193836);

-- Vps4 windows plan: Enhance (T2.1)  (spec: hosting.c2.r4.d60) Tier: 20
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=20 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1066929, 1066934, 1066937, 1066938, 1066941, 1066944, 1066947, 1066950);

-- Vps4 windows plan: 2.2  (spec: hosting.c2.r8.d100) Tier: 25
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=25 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193817, 1193822, 1193825, 1193826, 1193829, 1193832, 1193835, 1193838);

-- Vps4 windows plan: Grow (T3.1)  (spec: hosting.c3.r6.d150) Tier: 30
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=30 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1066952, 1066955, 1066957, 1066958, 1066960, 1066962, 1066964, 1066966);

-- Vps4 windows plan: Expand (T4.1)  (spec: hosting.c4.r8.d200) Tier: 40
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=40 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1066968, 1066971, 1066973, 1066974, 1066976, 1066978, 1066980, 1066982);

-- Vps4 windows plan: 4.2  (spec: hosting.c4.r16.d200) Tier: 41
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=41 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193840, 1193843, 1193845, 1193846, 1193848, 1193850, 1193852, 1193854);

-- Vps4 windows plan: 6.1  (spec: hosting.c6.r16.d300) Tier: 42
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=42 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193856, 1193859, 1193861, 1193862, 1193864, 1193866, 1193868, 1193870);

-- Vps4 windows plan: 6.2  (spec: hosting.c6.r24.d300) Tier: 43
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=43 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193872, 1193875, 1193877, 1193878, 1193880, 1193882, 1193884, 1193886);

-- Vps4 windows plan: 8.1  (spec: hosting.c8.r16.d400) Tier: 44
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=44 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193888, 1193891, 1193893, 1193894, 1193896, 1193898, 1193900, 1193902);

-- Vps4 windows plan: 8.2  (spec: hosting.c8.r32.d400) Tier: 45
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=45 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193904, 1193907, 1193909, 1193910, 1193912, 1193914, 1193916, 1193918);

-- Vps4 windows plan: 16.1  (spec: hosting.c16.r32.d800) Tier: 46
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=46 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193920, 1193923, 1193925, 1193926, 1193928, 1193930, 1193932, 1193934);

-- Vps4 windows plan: 16.2  (spec: hosting.c16.r64.d800) Tier: 47
UPDATE plan SET os_type_id=o.os_type_id, spec_id=s.spec_id
FROM os_type o, virtual_machine_spec s
WHERE s.tier=47 AND o.name='windows' AND s.valid_until='infinity'
AND pfid IN (1193936, 1193939, 1193941, 1193942, 1193944, 1193946, 1193948, 1193950);

