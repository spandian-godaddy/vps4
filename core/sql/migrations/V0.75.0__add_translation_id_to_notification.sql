ALTER TABLE notification_extended_details ADD COLUMN translation_id TEXT;
ALTER TABLE notification_extended_details ALTER COLUMN start_time DROP NOT NULL;
ALTER TABLE notification_extended_details ALTER COLUMN end_time DROP NOT NULL;
ALTER TABLE notification_extended_details ALTER COLUMN start_time DROP DEFAULT;
ALTER TABLE notification_extended_details ALTER COLUMN end_time DROP DEFAULT;