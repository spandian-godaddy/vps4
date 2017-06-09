ALTER TABLE ip_address DROP CONSTRAINT IF EXISTS ip_address_ip_address_key1;
ALTER TABLE ip_address DROP CONSTRAINT IF EXISTS ip_address_ip_address_key;
CREATE UNIQUE INDEX unique_active_ip ON ip_address (ip_address) WHERE valid_until='infinity';
