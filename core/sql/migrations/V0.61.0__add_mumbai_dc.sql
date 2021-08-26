INSERT INTO data_center (data_center_id, description) values (5, 'bom');
ALTER TABLE virtual_machine ADD COLUMN data_center_id INTEGER REFERENCES data_center(data_center_id);