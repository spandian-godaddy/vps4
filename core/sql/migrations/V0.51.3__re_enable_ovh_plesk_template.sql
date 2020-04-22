-- Re-enable Plesk OVH template
UPDATE image SET valid_until = 'infinity' WHERE hfs_name IN ('centos7-plesk17_64');

-- Disable custom template
UPDATE image SET valid_until = now_utc() WHERE hfs_name IN ('centos-7-plesk-single');
