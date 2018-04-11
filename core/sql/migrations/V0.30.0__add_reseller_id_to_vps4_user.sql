ALTER TABLE vps4_user ADD COLUMN reseller_id VARCHAR(255);
UPDATE vps4_user SET reseller_id = '1';
ALTER TABLE vps4_user ALTER COLUMN reseller_id SET NOT NULL;
