DROP FUNCTION ip_address_create(BIGINT, UUID, VARCHAR(15), INT, BIGINT);

CREATE OR REPLACE FUNCTION ip_address_create(
        p_ip_address_id      BIGINT,
        p_vm_id              UUID,
        p_ip_address         VARCHAR(15),
        p_ip_address_type_id INT
    )

    RETURNS VOID AS $$
BEGIN

    INSERT INTO ip_address (ip_address_id, ip_address, vm_id, ip_address_type_id)
       VALUES (p_ip_address_id, p_ip_address, p_vm_id, p_ip_address_type_id);

END;
$$ LANGUAGE plpgsql;

ALTER TABLE ip_address DROP mail_relay_id;