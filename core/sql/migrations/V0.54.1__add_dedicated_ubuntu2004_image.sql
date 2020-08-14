-- Retire Ubuntu 16.04 image
UPDATE image SET valid_until = now_utc() WHERE hfs_name = 'ubuntu1604-server_64';

-- Replace with new Ubuntu 20.04 image
INSERT INTO image (hfs_name, control_panel_id, os_type_id, name, server_type_id)
VALUES ('ubuntu2004-server_64', 0, 1, 'Ubuntu 20.04', 2);
