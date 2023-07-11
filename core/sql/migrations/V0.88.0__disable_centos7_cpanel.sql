-- Disable centos7 cPanel across all platforms
UPDATE image SET valid_until = now_utc() WHERE hfs_name IN ('vps4-centos-7-cpanel-11', 'centos7-cpanel-latest_64', 'hfs-centos7-cpanel');
