-- Add hfs-almalinux9-plesk image for OH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-almalinux9-plesk', control_panel_id, os_type_id, 'AlmaLinux 9 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';
