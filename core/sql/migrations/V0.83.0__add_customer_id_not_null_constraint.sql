-- Remove test vps4 users, Add not null constraint to customer id column
DELETE FROM vps4_user WHERE shopper_id = 'Support';
DELETE FROM vps4_user WHERE shopper_id = 'TestUser';
DELETE FROM vps4_user WHERE shopper_id = 'SomeUser';
ALTER TABLE vps4_user
ALTER COLUMN customer_id SET NOT NULL;