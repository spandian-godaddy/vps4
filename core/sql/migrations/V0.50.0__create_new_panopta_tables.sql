CREATE TABLE panopta_customer
(
    partner_customer_key VARCHAR(255) NOT NULL PRIMARY KEY,
    customer_key         VARCHAR(255) NOT NULL,
    created              TIMESTAMP    NOT NULL DEFAULT now_utc(),
    destroyed            TIMESTAMP             DEFAULT 'infinity'
);

CREATE TABLE panopta_server
(
    id                   SERIAL PRIMARY KEY,
    partner_customer_key VARCHAR(255) NOT NULL REFERENCES panopta_customer(partner_customer_key),
    vm_id                UUID         NOT NULL REFERENCES virtual_machine (vm_id),
    server_id            BIGINT       NOT NULL,
    server_key           VARCHAR(255) NOT NULL,
    created              TIMESTAMP    NOT NULL DEFAULT now_utc(),
    destroyed            TIMESTAMP             DEFAULT 'infinity'
);

CREATE INDEX panopta_server_vm_id_idx ON panopta_server (vm_id);
CREATE INDEX panopta_server_server_key_idx ON panopta_server (server_key);