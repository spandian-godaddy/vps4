SELECT '_dc_', vu.shopper_id, COUNT(vm.vm_id) AS numOfVms
FROM virtual_machine vm
  JOIN project prj USING (project_id)
  JOIN vps4_user vu USING (vps4_user_id)
WHERE vm.canceled ='infinity'
 AND vm.valid_until = 'infinity'
 AND LENGTH(vu.shopper_id) > 3
 AND vu.shopper_id NOT IN (
  '6203314',
  '177999314',
  '107294123',
  '186668834',
  '186733903',
  '158496190',
  '177277701',
  '101606424',
  '190111365', '101606424', '191093037', '187820995')
GROUP BY vu.shopper_id
  HAVING COUNT(*) > 1
ORDER BY numOfVms DESC