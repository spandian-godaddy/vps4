-- Disable hfs centos-7 cpanel image
UPDATE image SET valid_until = now_utc() WHERE hfs_name='hfs-centos-7-cpanel-11';

-- Add vps4 centos-7 cpanel-11 image
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'vps4-centos-7-cpanel-11', control_panel_id, os_type_id, 'CentOS 7 (cPanel)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='cpanel' AND os.name='linux' AND st.platform='OPENSTACK';
