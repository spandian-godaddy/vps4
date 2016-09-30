CREATE OR REPLACE FUNCTION ip_address_create(
    p_ip_address_id BIGINT,
    p_project_id BIGINT
	)

    RETURNS VOID AS $$
BEGIN

	INSERT INTO ip_address (ip_address_id, project_id)
	   VALUES (p_ip_address_id, p_project_id);

END;
$$ LANGUAGE plpgsql;
