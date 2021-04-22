DROP FUNCTION IF EXISTS ip_address_create(BIGINT, UUID, VARCHAR(15), INT);
DROP FUNCTION IF EXISTS ip_address_delete(BIGINT);

ALTER TABLE ip_address RENAME COLUMN ip_address_id TO hfs_address_id;
ALTER TABLE ip_address RENAME COLUMN id TO address_id;