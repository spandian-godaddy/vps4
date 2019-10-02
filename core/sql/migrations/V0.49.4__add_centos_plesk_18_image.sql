-- Disable centos-7 plesk-17 image
UPDATE image SET valid_until = now_utc() WHERE hfs_name='vps4-centos-7-plesk-17';

-- Add centos-7 plesk-18 image
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'vps4-centos-7-plesk-18', control_panel_id, os_type_id, 'CentOS 7 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='linux' AND st.platform='OPENSTACK';
