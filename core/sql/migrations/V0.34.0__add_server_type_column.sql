CREATE TABLE server_type (
  server_type_id  SERIAL  PRIMARY KEY,
  server_type     TEXT    NOT NULL,
  platform        TEXT    NOT NULL
);

INSERT INTO server_type(server_type, platform) VALUES ('VIRTUAL', 'OPENSTACK');
INSERT INTO server_type(server_type, platform) VALUES ('DEDICATED', 'OVH');

ALTER TABLE virtual_machine_spec
    ADD COLUMN server_type_id INTEGER
    NOT NULL
    DEFAULT 1
    REFERENCES server_type(server_type_id);

ALTER TABLE image
    ADD COLUMN server_type_id INTEGER
    NOT NULL
    DEFAULT 1
    REFERENCES server_type(server_type_id);
