ALTER TABLE panopta_server ADD UNIQUE (server_id);
CREATE TABLE panopta_additional_fqdns
(
    additional_fqdn_id    SERIAL PRIMARY KEY,
    server_id             BIGINT NOT NULL REFERENCES panopta_server(server_id),
    fqdn                  VARCHAR(255) NOT NULL,
    valid_on              TIMESTAMP NOT NULL DEFAULT now_utc(),
    valid_until           TIMESTAMP NOT NULL DEFAULT 'infinity'
);