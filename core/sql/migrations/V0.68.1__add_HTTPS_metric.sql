INSERT INTO metric (id, name, metric_group_id) VALUES (11, 'HTTPS', 2);

DROP TABLE IF EXISTS metric_type;

CREATE TABLE metric_type (
                             id SERIAL NOT NULL PRIMARY KEY,
                             metric_type_id  BIGINT,
                             metric_id  INTEGER REFERENCES metric (id),
                             os_type_id INTEGER REFERENCES os_type (os_type_id)
);

INSERT INTO metric_type (metric_type_id, metric_id, os_type_id) VALUES
                                                                    (1085,  (SELECT id FROM metric WHERE name = 'CPU'), 1),
                                                                    (1015,  (SELECT id FROM metric WHERE name = 'CPU'), 2),
                                                                    (675,   (SELECT id FROM metric WHERE name = 'RAM'), 1),
                                                                    (2317,  (SELECT id FROM metric WHERE name = 'RAM'), 2),
                                                                    (575,   (SELECT id FROM metric WHERE name = 'DISK'), 1),
                                                                    (2297,  (SELECT id FROM metric WHERE name = 'DISK'), 2),

                                                                    (11,    (SELECT id FROM metric WHERE name = 'PING'), 1),
                                                                    (11,    (SELECT id FROM metric WHERE name = 'PING'), 2),

                                                                    (111,   (SELECT id FROM metric WHERE name = 'FTP'), 1),
                                                                    (111,   (SELECT id FROM metric WHERE name = 'FTP'), 2),

                                                                    (311,   (SELECT id FROM metric WHERE name = 'SSH'), 1),
                                                                    (311,   (SELECT id FROM metric WHERE name = 'SSH'), 2),

                                                                    (271,   (SELECT id FROM metric WHERE name = 'SMTP'), 1),
                                                                    (271,   (SELECT id FROM metric WHERE name = 'SMTP'), 2),

                                                                    (31,    (SELECT id FROM metric WHERE name = 'HTTP'), 1),
                                                                    (31,    (SELECT id FROM metric WHERE name = 'HTTP'), 2),

                                                                    (221,   (SELECT id FROM metric WHERE name = 'POP3'), 1),
                                                                    (221,   (SELECT id FROM metric WHERE name = 'POP3'), 2),

                                                                    (131,   (SELECT id FROM metric WHERE name = 'IMAP'), 1),
                                                                    (131,   (SELECT id FROM metric WHERE name = 'IMAP'), 2),

                                                                    (81,   (SELECT id FROM metric WHERE name = 'HTTPS'), 1),
                                                                    (81,   (SELECT id FROM metric WHERE name = 'HTTPS'), 2);