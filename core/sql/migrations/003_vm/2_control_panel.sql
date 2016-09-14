DROP TABLE IF EXISTS control_panel;

CREATE TABLE control_panel
(
      name TEXT NOT NULL UNIQUE
    , control_panel_id serial PRIMARY KEY
);

INSERT INTO control_panel(name, control_panel_id) VALUES ('cpanel', 1);
INSERT INTO control_panel(name, control_panel_id) VALUES ('plesk', 2);