DROP TABLE IF EXISTS control_panel;

CREATE TABLE control_panel
(
      name TEXT NOT NULL UNIQUE
    , controlpanel_id serial PRIMARY KEY
);