CREATE TABLE data_center (
    data_center_id	INT,
  	description		VARCHAR(255),
  	
  	PRIMARY KEY (data_center_id)
);

INSERT INTO data_center (data_center_id, description)
    VALUES (1, 'phx3'), (2, 'a2');