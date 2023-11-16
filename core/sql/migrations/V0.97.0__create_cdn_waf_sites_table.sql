CREATE TABLE vm_firewall_site (
    id                  SERIAL          PRIMARY KEY,
    vm_id               UUID            NOT NULL    REFERENCES virtual_machine(vm_id),
    ip_address_id       BIGINT          NOT NULL    REFERENCES ip_address(address_id),
    domain              VARCHAR(255)    NOT NULL,
    site_id             VARCHAR(255)    UNIQUE NOT NULL,
    valid_on            TIMESTAMP       NOT NULL    DEFAULT NOW_UTC(),
    valid_until         TIMESTAMP       NOT NULL    DEFAULT 'infinity'
);