DROP TABLE IF EXISTS os_type;

CREATE TABLE os_type
(
      name TEXT NOT NULL UNIQUE
    , os_type_id serial PRIMARY KEY
);

INSERT INTO os_type(name, os_type_id) VALUES ('linux', 1);
INSERT INTO os_type(name, os_type_id) VALUES ('windows', 2);