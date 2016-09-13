DROP TABLE IF EXISTS control_panel;

CREATE TABLE control_panel
(
      name TEXT NOT NULL UNIQUE
    , control_panel_id serial PRIMARY KEY
);