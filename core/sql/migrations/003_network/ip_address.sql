CREATE TABLE ip_address (
    ip_address_id   BIGINT      PRIMARY KEY,
    project_id      BIGINT      NOT NULL REFERENCES project(project_id),
    valid_on        TIMESTAMP   NOT NULL DEFAULT NOW(),
    valid_until     TIMESTAMP   NOT NULL DEFAULT 'infinity'
);
 
CREATE INDEX ipaddress_project_id_idx ON ip_address (project_id);