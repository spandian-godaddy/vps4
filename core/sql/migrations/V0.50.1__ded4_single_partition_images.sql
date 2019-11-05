UPDATE image SET valid_until = now_utc() WHERE hfs_name IN (
    'centos7_64',
    'ubuntu1604-server_64',
    'debian8_64',
    'centos7-cpanel-latest_64',
    'centos7-plesk17_64');

INSERT INTO image
  (hfs_name, control_panel_id, os_type_id, name, server_type_id)
VALUES
  ('centos-7-single', 0, 1, 'CentOS 7', 2),
  ('centos-7-cpanel-single', 1, 1, 'CentOS 7 (cPanel)', 2),
  ('centos-7-plesk-single', 2, 1, 'CentOS 7 (Plesk)', 2),
  ('debian-8-single', 0, 1, 'Debian 8', 2),
  ('ubuntu-1604-single', 0, 1, 'Ubuntu 16.04', 2);

