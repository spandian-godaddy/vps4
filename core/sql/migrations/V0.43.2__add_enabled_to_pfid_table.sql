ALTER TABLE plan ADD enabled BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE plan SET enabled = TRUE WHERE control_panel_id is NULL;
