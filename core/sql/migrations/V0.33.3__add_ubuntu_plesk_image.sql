-- V0.29.0__add_windows_images.sql updated the ids directly, and did not increment the sequence
--  This will reset the sequence to where it should be
ALTER SEQUENCE image_image_id_seq RESTART WITH 12;
INSERT INTO image
  (hfs_name, control_panel_id, os_type_id, name)
VALUES
  ('hfs-ubuntu-1604-plesk-17', 2, 1, 'Ubuntu 16.04 (Plesk)');
