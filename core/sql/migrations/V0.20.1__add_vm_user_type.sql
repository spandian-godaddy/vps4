CREATE TABLE vm_user_type (
  type_id      INTEGER       PRIMARY KEY,
  type_name    VARCHAR(255)
);

INSERT INTO vm_user_type(type_id, type_name)    VALUES (1, 'CUSTOMER');
INSERT INTO vm_user_type(type_id, type_name)    VALUES (2, 'SUPPORT');

ALTER TABLE vm_user
  ADD COLUMN vm_user_type_id      INT     NOT NULL  DEFAULT 1      REFERENCES vm_user_type(type_id);

DROP FUNCTION IF EXISTS user_create(TEXT, UUID, BOOLEAN);
