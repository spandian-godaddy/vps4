-- Add AlmaLinux 8 MYH image for OH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-almalinux8', control_panel_id, os_type_id, 'AlmaLinux 8', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='myh' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';

-- Add AlmaLinux 8 cPanel image for OH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-almalinux8-cpanel', control_panel_id, os_type_id, 'AlmaLinux 8 (cPanel)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='cpanel' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';

-- Add AlmaLinux 8 cPanel image for OH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-almalinux8-plesk', control_panel_id, os_type_id, 'AlmaLinux 8 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';
