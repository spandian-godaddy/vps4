ALTER TABLE vps4_user ADD COLUMN customer_id UUID UNIQUE;
drop function get_or_create_user(text, text);
drop function get_or_create_user(text);