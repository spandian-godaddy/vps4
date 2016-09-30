CREATE OR REPLACE FUNCTION ip_address_delete(
    p_ip_address_id BIGINT
	)

    RETURNS VOID AS $$
BEGIN
	
	 UPDATE ip_address SET valid_until = NOW();

END;
$$ LANGUAGE plpgsql;
