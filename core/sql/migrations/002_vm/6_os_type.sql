DROP TABLE IF EXISTS os_type;

CREATE TABLE os_type
(
      name TEXT NOT NULL UNIQUE
    , os_type_id serial PRIMARY KEY
);