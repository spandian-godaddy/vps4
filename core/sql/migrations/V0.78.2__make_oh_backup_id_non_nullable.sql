ALTER TABLE oh_backup ALTER COLUMN oh_backup_id SET NOT NULL;
ALTER TABLE oh_backup ADD UNIQUE (oh_backup_id);
