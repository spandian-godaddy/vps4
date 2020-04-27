-- Rename Windows OVH template to match OVH name change
INSERT INTO image
  (hfs_name, control_panel_id, os_type_id, name, server_type_id)
VALUES
  ('win2016-std-new_64', 0, 2, 'Windows 2016 Standard', 2);

-- Disable previous OVH Windows template
UPDATE image SET valid_until = now_utc() WHERE hfs_name IN ('win2016-std_64');
