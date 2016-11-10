CREATE TABLE ip_address (
    ip_address_id       BIGINT      PRIMARY KEY,
    vm_id               BIGINT      NOT NULL REFERENCES virtual_machine(vm_id),
    ip_address          VARCHAR(15) NOT NULL,
    ip_address_type_id  INT         NOT NULL REFERENCES ip_address_type(id),
    valid_on            TIMESTAMP   NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMP   NOT NULL DEFAULT 'infinity'
);
 
CREATE INDEX ip_address_vm_id_idx ON ip_address (vm_id);