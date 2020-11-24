-- Disable initial Optimized Hosting PoC test images
UPDATE image SET valid_until = now_utc() WHERE hfs_name='hfs-centos70-x86_64-vmtempl';
UPDATE image SET valid_until = now_utc() WHERE hfs_name='hfs-centos70-cpanel-11-x86_64-vmtempl';

-- Add official images for Optimized Hosting.
INSERT INTO image
  (hfs_name, control_panel_id, os_type_id, name, server_type_id)
VALUES
  ('hfs-centos7', 0, 1, 'CentOS 7', 3),
  ('hfs-centos7-cpanel', 1, 1, 'CentOS 7 (cPanel)', 3),
  ('hfs-centos7-plesk', 2, 1, 'CentOS 7 (Plesk)', 3),
  ('hfs-ubuntu2004', 0, 1, 'Ubuntu 20.04', 3),
  ('hfs-debian10', 0, 1, 'Debian 10', 3),
  ('hfs-centos8', 0, 1, 'CentOS 8', 3);

