ALTER TABLE credit ADD COLUMN monitoring INT NOT NULL DEFAULT 0;
ALTER TABLE ip_address ADD ping_check_id BIGINT;
ALTER TABLE ip_address ADD UNIQUE (ping_check_id);
