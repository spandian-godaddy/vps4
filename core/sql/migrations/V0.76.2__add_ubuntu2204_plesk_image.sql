-- Add ubuntu-22.04 plesk image
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
SELECT 'hfs-ubuntu2204-plesk', control_panel_id, os_type_id, 'Ubuntu 22.04 (Plesk)', server_type_id
FROM control_panel cp, os_type os, server_type st
WHERE cp.name='plesk' AND os.name='linux' AND st.platform='OPTIMIZED_HOSTING';

