-- Disable hfs windows-2016 plesk image
UPDATE image SET valid_until = now_utc() WHERE hfs_name='hfs-windows-2016-plesk-17';

-- Add vps4 windows-2016 plesk-18 image
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'vps4-windows-2016-plesk-18', control_panel_id, os_type_id, 'Windows 2016 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='windows' AND st.platform='OPENSTACK'
AND NOT EXISTS (SELECT image_id FROM image WHERE hfs_name='vps4-windows-2016-plesk-18');
