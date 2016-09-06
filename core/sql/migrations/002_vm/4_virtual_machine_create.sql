DROP FUNCTION IF EXISTS virtual_machine_create(p_vm_id int8, p_sgid int8, p_spec_name varchar);

CREATE OR REPLACE FUNCTION virtual_machine_create(
    p_vm_id BIGINT,
    p_sgid BIGINT,
    p_spec_name virtual_machine_spec.spec_name%TYPE,
    p_name VARCHAR(255)
)
    RETURNS VOID AS $$
DECLARE
    vm_spec_id   INT;
BEGIN
	
	-- look up the vm_spec_id
	SELECT spec_id FROM virtual_machine_spec WHERE spec_name=p_spec_name INTO vm_spec_id;
	IF NOT FOUND THEN
	   RAISE 'Unknown Virtual Machine spec: %', p_spec_name;
	END IF;
	
	INSERT INTO virtual_machine (vm_id, sgid, spec_id, name)
	   VALUES (p_vm_id, p_sgid, vm_spec_id, p_name);

END;
$$ LANGUAGE plpgsql;