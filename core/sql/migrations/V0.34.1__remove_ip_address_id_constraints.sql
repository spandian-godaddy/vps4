ALTER TABLE ip_address DROP CONSTRAINT ip_address_pkey;
ALTER TABLE ip_address ALTER COLUMN ip_address_id DROP NOT NULL;