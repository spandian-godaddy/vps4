
CREATE TABLE os_type
(
    os_type_id INT PRIMARY KEY
    , name TEXT NOT NULL UNIQUE 
);

INSERT INTO os_type(os_type_id, name) VALUES (1, 'linux');
INSERT INTO os_type(os_type_id, name) VALUES (2, 'windows');