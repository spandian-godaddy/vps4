CREATE OR REPLACE FUNCTION user_create(p_name TEXT 
	, p_vm_id BIGINT
	, p_admin_enabled BOOLEAN)

    RETURNS VOID AS $$
DECLARE
BEGIN
	
	INSERT INTO vm_user (vm_id, name, admin_enabled)
	   VALUES (p_vm_id, p_name, p_admin_enabled);
	   
END;
$$ LANGUAGE plpgsql;