-- Add column to indicate the timestamp of Nydus warning acknowledgement from customer
ALTER TABLE virtual_machine ADD COLUMN nydus_warning_ack TIMESTAMP DEFAULT 'infinity';

-- Disable OH CentOS8 image
UPDATE image SET valid_until = now_utc() WHERE hfs_name='hfs-centos8';
