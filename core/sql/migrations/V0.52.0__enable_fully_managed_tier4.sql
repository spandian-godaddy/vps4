update plan set enabled = true from virtual_machine_spec spec where plan.spec_id = spec.spec_id and spec.spec_name = 'hosting.c4.r8.d200' and plan.enabled = false;
