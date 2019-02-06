ALTER SEQUENCE image_image_id_seq RESTART WITH 20;
INSERT INTO image
(hfs_name, control_panel_id, os_type_id, name, server_type_id)
VALUES
('debian8_64', 0, 1, 'Debian 8', 2);