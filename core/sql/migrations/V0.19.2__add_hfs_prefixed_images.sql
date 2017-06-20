-- Fix image sequence
ALTER SEQUENCE image_image_id_seq RESTART WITH 5;

INSERT INTO image
  (hfs_name, control_panel_id, os_type_id, name)
VALUES
  ('hfs-centos-7', 0, 1, 'CentOS 7'),
  ('hfs-windows-2012r2', 0, 2, 'Windows 2012 R2'),
  ('hfs-centos-7-cpanel-11', 1, 1, 'CentOS 7 (cPanel 11)'),
  ('hfs-windows-2012r2-plesk-12.5', 2, 2, 'Windows 2012 R2 (Plesk 17.0.17)');
