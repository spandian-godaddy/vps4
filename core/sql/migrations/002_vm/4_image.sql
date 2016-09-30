
CREATE TABLE image
(
	image_id 		   bigserial PRIMARY KEY
	, name 			   TEXT NOT NULL UNIQUE
	, control_panel_id INTEGER NOT NULL REFERENCES control_panel (control_panel_id)
	, os_type_id	   INTEGER NOT NULL REFERENCES os_type (os_type_id)
);
