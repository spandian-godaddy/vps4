UPDATE virtual_machine_spec SET valid_until=now_utc() WHERE spec_name IN ('hosting.c3.r6.d90', 'hosting.c4.r8.d120');
UPDATE virtual_machine_spec SET valid_until='infinity' WHERE spec_name IN ('hosting.c3.r6.d150', 'hosting.c4.r8.d200');
