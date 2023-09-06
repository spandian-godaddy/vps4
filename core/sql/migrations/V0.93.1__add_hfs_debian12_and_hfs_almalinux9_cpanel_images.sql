-- Add Debian 12 MYH image for OH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-debian12', control_panel_id, os_type_id, 'Debian 12', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='myh' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';

-- Add hfs-almalinux9-cpanel image for OH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-almalinux9-cpanel', control_panel_id, os_type_id, 'AlmaLinux 9 (cPanel)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='cpanel' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';
