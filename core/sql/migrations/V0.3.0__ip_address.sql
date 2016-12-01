
CREATE TABLE ip_address_type (
    id   INT PRIMARY KEY,
    name TEXT   NOT NULL
);

CREATE TABLE ip_address (
    ip_address_id       BIGINT      PRIMARY KEY,
    vm_id               BIGINT      NOT NULL REFERENCES virtual_machine(vm_id),
    ip_address          VARCHAR(15) NOT NULL,
    ip_address_type_id  INT         NOT NULL REFERENCES ip_address_type(id),
    valid_on            TIMESTAMP   NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMP   NOT NULL DEFAULT 'infinity'
);
 
CREATE INDEX ip_address_vm_id_idx ON ip_address (vm_id);



INSERT INTO ip_address_type (id, name) VALUES (1, 'primary');
INSERT INTO ip_address_type (id, name) VALUES (2, 'secondary');


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


CREATE OR REPLACE FUNCTION ip_address_delete(
    p_ip_address_id BIGINT
    )

    RETURNS VOID AS $$
BEGIN
    
     UPDATE ip_address SET valid_until = NOW() WHERE ip_address_id = p_ip_address_id;

END;
$$ LANGUAGE plpgsql;


