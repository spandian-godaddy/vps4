--modify ip address counts for currently valid OH and OVH images based on tier table

--NOTE: ip address count INCLUDES primary ip and secondary ips

--Optimized Hosting VPS
UPDATE virtual_machine_spec
SET ip_address_count = 2
WHERE tier BETWEEN 5 AND 15
  AND server_type_id=3
  AND valid_until='infinity';

UPDATE virtual_machine_spec
SET ip_address_count = 3
WHERE tier BETWEEN 20 AND 25
  AND server_type_id=3
  AND valid_until='infinity';


UPDATE virtual_machine_spec
SET ip_address_count = 4
WHERE tier BETWEEN 30 AND 47
  AND server_type_id=3
  AND valid_until='infinity';

--Dedicated
UPDATE virtual_machine_spec
SET ip_address_count = 2
WHERE (tier = 60 OR tier = 120)
  AND valid_until='infinity';


UPDATE virtual_machine_spec
SET ip_address_count = 3
WHERE (tier = 80 OR tier = 140)
    AND valid_until='infinity';

UPDATE virtual_machine_spec
SET ip_address_count = 4
WHERE (tier = 100 OR tier = 110 OR tier = 160 OR tier = 170)
    AND valid_until='infinity';