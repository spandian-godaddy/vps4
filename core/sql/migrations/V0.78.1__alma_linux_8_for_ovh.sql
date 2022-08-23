-- Add AlmaLinux 8 MYH image for OVH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'alma8_64', control_panel_id, os_type_id, 'AlmaLinux 8', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='myh' AND os.name='linux' AND st.platform='OVH';

-- Add AlmaLinux 8 cPanel image for OVH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'alma8-cpanel-latest_64', control_panel_id, os_type_id, 'AlmaLinux 8 (cPanel)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='cpanel' AND os.name='linux' AND st.platform='OVH';

-- Add AlmaLinux 8 Plesk image for OVH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'alma8-plesk18_64', control_panel_id, os_type_id, 'AlmaLinux 8 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='linux' AND st.platform='OVH';
