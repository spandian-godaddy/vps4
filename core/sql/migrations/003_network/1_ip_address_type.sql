CREATE TABLE ip_address_type (
    id   INT PRIMARY KEY,
    name TEXT   NOT NULL
);

INSERT INTO ip_address_type (id, name) VALUES (1, 'primary');
INSERT INTO ip_address_type (id, name) VALUES (2, 'secondary');