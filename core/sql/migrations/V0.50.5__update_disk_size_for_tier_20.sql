-- Deprecate 60 GB disk specs
UPDATE virtual_machine_spec SET valid_until=now_utc() WHERE spec_name IN ('hosting.c2.r4.d60', 'hosting.c2.r8.d60');

-- Re-enable 100 GB disk specs
UPDATE virtual_machine_spec SET valid_until='infinity' WHERE spec_name IN ('hosting.c2.r4.d100', 'hosting.c2.r8.d100');

-- Make all hosting.c2.r4.d60 plans point to hosting.c2.r4.d100
UPDATE plan
SET spec_id = (
	SELECT spec_id
	FROM virtual_machine_spec vms
	WHERE spec_name='hosting.c2.r4.d100'
)
WHERE spec_id IN (
	SELECT spec_id
	FROM virtual_machine_spec vms
	WHERE spec_name='hosting.c2.r4.d60'
);

-- Make all hosting.c2.r8.d60 plans point to hosting.c2.r8.d100
UPDATE plan
SET spec_id = (
	SELECT spec_id
	FROM virtual_machine_spec vms
	WHERE spec_name='hosting.c2.r8.d100'
)
WHERE spec_id IN (
	SELECT spec_id
	FROM virtual_machine_spec vms
	WHERE spec_name='hosting.c2.r8.d60'
);
