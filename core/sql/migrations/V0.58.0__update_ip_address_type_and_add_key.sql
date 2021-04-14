ALTER TABLE ip_address ADD COLUMN id BIGSERIAL;
ALTER TABLE ip_address ALTER COLUMN ip_address TYPE inet using ip_address::inet;
ALTER TABLE ip_address ADD PRIMARY KEY (id);
DROP FUNCTION IF EXISTS ip_address_create(BIGINT, UUID, VARCHAR(15), INT, BIGINT);