-- Add ubuntu-20.24 vanilla image
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-ubuntu2204', control_panel_id, os_type_id, 'Ubuntu 22.04', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='myh' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';
