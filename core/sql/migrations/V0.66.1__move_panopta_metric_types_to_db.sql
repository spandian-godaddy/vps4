ALTER TABLE metric_type RENAME TO metric_group;

ALTER TABLE metric RENAME COLUMN metric_type_id TO metric_group_id;

CREATE TABLE metric_type (
    metric_type_id  BIGINT PRIMARY KEY,
    metric_id  INTEGER REFERENCES metric (id)
);

INSERT INTO metric_type (metric_type_id, metric_id) VALUES
    (1085,  (SELECT id FROM metric WHERE name = 'CPU')),
    (1015,  (SELECT id FROM metric WHERE name = 'CPU')),
    (675,   (SELECT id FROM metric WHERE name = 'RAM')),
    (2317,  (SELECT id FROM metric WHERE name = 'RAM')),
    (575,   (SELECT id FROM metric WHERE name = 'DISK')),
    (2297,  (SELECT id FROM metric WHERE name = 'DISK')),
    (11,    (SELECT id FROM metric WHERE name = 'PING')),
    (111,   (SELECT id FROM metric WHERE name = 'FTP')),
    (311,   (SELECT id FROM metric WHERE name = 'SSH')),
    (271,   (SELECT id FROM metric WHERE name = 'SMTP')),
    (31,    (SELECT id FROM metric WHERE name = 'HTTP')),
    (221,   (SELECT id FROM metric WHERE name = 'POP3')),
    (131,   (SELECT id FROM metric WHERE name = 'IMAP'));