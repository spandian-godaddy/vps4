-- Re-enable OVH templates
UPDATE image SET valid_until = 'infinity' WHERE hfs_name IN (
    'centos7_64',
    'ubuntu1604-server_64',
    'debian8_64',
    'centos7-cpanel-latest_64');

-- Disable custom templates
UPDATE image SET valid_until = now_utc() WHERE hfs_name IN (
    'centos-7-single',
    'centos-7-cpanel-single',
    'debian-8-single',
    'ubuntu-1604-single');
