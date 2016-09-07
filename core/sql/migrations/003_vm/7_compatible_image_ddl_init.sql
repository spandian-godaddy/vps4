DROP TABLE IF EXISTS compatible_image;

CREATE TABLE compatible_image
(
	image_id 		   bigserial PRIMARY KEY
	, name 			   TEXT NOT NULL UNIQUE
	, control_panel_id INTEGER REFERENCES control_panel (controlpanel_id)
	, os_type_id	   INTEGER REFERENCES os_type (os_type_id)
);
