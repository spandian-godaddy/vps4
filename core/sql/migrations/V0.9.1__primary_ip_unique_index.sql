CREATE UNIQUE INDEX unique_primary_ip ON ip_address (vm_id) WHERE ip_address_type_id=1;
ALTER TABLE ip_address ADD UNIQUE (ip_address);