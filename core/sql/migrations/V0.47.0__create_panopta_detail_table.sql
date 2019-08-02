CREATE TABLE panopta_detail
(
    panopta_detail_id    SERIAL PRIMARY KEY,
    vm_id                UUID         NOT NULL REFERENCES vps4.public.virtual_machine(vm_id),
    partner_customer_key VARCHAR(255) NOT NULL,
    customer_key         VARCHAR(255) NOT NULL,
    server_id            BIGINT,
    server_key           VARCHAR(255),
    created              TIMESTAMP NOT NULL DEFAULT now_utc(),
    destroyed            TIMESTAMP
);
