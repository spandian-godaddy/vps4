-- Add Windows 2022 image for OVH
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
    SELECT 'win2022-std_64', control_panel_id, os_type_id, 'Windows 2022 Standard', server_type_id
    FROM control_panel cp, os_type os, server_type st
    WHERE cp.name='myh' AND os.name='windows' AND st.platform='OVH';
