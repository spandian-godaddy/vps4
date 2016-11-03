
CREATE TABLE control_panel
(
    control_panel_id INT PRIMARY KEY
    , name TEXT NOT NULL UNIQUE
);

INSERT INTO control_panel(control_panel_id, name) VALUES (0, 'none');
INSERT INTO control_panel(control_panel_id, name) VALUES (1, 'cpanel');
INSERT INTO control_panel(control_panel_id, name) VALUES (2, 'plesk');