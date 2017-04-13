ALTER TABLE ip_address ADD ping_check_id BIGINT;
ALTER TABLE ip_address ADD UNIQUE (ping_check_id);

CREATE OR REPLACE FUNCTION update_ip_address_add_ping_check_id(
        p_ip_address_id BIGINT,
        p_ping_check_id BIGINT
    )

    RETURNS VOID AS $$
BEGIN

    UPDATE ip_address SET ping_check_id = p_ping_check_id WHERE ip_address_id = p_ip_address_id;

END;
$$ LANGUAGE plpgsql;