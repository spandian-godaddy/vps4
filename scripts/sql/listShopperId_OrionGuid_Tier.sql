select '_dc_' as dc, vu.shopper_id, vm.orion_guid, vmspec.tier from virtual_machine vm
  join project prj USING (project_id)
  join vps4_user vu USING (vps4_user_id)
  join virtual_machine_spec vmspec USING (spec_id)
where vm.canceled ='infinity'
 and vm.valid_until = 'infinity'
 and length(vu.shopper_id) > 3
 and vu.shopper_id not in (
  '6203314', '177999314', '107294123', '186668834',
  '186733903', '158496190', '177277701',  '101606424',
  '190111365', '101606424', '191093037', '187820995')