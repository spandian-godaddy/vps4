-- Add ubuntu-22.04 vanilla image for OVH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'ubuntu2204-server_64', control_panel_id, os_type_id, 'Ubuntu 22.04', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='myh' AND os.name='linux' AND st.platform='OVH';
