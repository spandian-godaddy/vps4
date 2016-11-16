CREATE OR REPLACE FUNCTION user_create(p_name TEXT 
	, p_virtual_machine_id BIGINT
	, p_admin_enabled BOOLEAN)

    RETURNS VOID AS $$
DECLARE
BEGIN
	
	INSERT INTO vm_user (virtual_machine_id, name, admin_enabled)
	   VALUES (p_virtual_machine_id, p_name, p_admin_enabled);
	   
END;
$$ LANGUAGE plpgsql;