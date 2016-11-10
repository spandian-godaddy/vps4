CREATE OR REPLACE FUNCTION ip_address_create(
	    p_ip_address_id      BIGINT,
	    p_vm_id              BIGINT,
	    p_ip_address         VARCHAR(15),
	    p_ip_address_type_id INT
	)

    RETURNS VOID AS $$
BEGIN

	-- if this is primary address, change any other primary addresses to secondary
	IF p_ip_address_type_id = 1 THEN
	   UPDATE ip_address SET ip_address_type_id = 2 WHERE vm_id = p_vm_id AND ip_address_type_id = 1;
	END IF;
	
	INSERT INTO ip_address (ip_address_id, ip_address, vm_id, ip_address_type_id)
	   VALUES (p_ip_address_id, p_ip_address, p_vm_id, p_ip_address_type_id);

END;
$$ LANGUAGE plpgsql;
