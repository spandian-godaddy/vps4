-- Disable centos-7 plesk-17 image for OVH
UPDATE image SET valid_until = now_utc() WHERE hfs_name='centos7-plesk17_64';

-- Add centos-7 plesk-18 image for OVH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'centos7-plesk18_64', control_panel_id, os_type_id, 'CentOS 7 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='linux' AND st.platform='OVH';
