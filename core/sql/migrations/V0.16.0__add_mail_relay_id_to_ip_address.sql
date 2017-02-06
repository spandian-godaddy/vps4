ALTER TABLE ip_address ADD mail_relay_id BIGINT;

DROP FUNCTION ip_address_create(BIGINT, UUID, VARCHAR(15), INT);

CREATE UNIQUE INDEX unique_primary_ip ON ip_address (vm_id) WHERE ip_address_type_id=1;
ALTER TABLE ip_address ADD UNIQUE (ip_address);
ALTER TABLE ip_address ADD UNIQUE (mail_relay_id);

CREATE OR REPLACE FUNCTION ip_address_create(
        p_ip_address_id      BIGINT,
        p_vm_id              UUID,
        p_ip_address         VARCHAR(15),
        p_ip_address_type_id INT,
        p_mail_relay_id      BIGINT
    )

    RETURNS VOID AS $$
BEGIN

    INSERT INTO ip_address (ip_address_id, ip_address, vm_id, ip_address_type_id, mail_relay_id)
       VALUES (p_ip_address_id, p_ip_address, p_vm_id, p_ip_address_type_id, p_mail_relay_id);

END;
$$ LANGUAGE plpgsql;