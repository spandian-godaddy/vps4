CREATE TABLE snapshot_type (
    snapshot_type_id  INT,
    snapshot_type     VARCHAR(64),
    
    PRIMARY KEY (snapshot_type_id)
);

INSERT INTO snapshot_type (snapshot_type_id, snapshot_type)
    VALUES (1, 'ON_DEMAND'), (2, 'AUTOMATIC');

ALTER TABLE snapshot ADD COLUMN snapshot_type_id INT REFERENCES snapshot_type(snapshot_type_id) NOT NULL DEFAULT 1;