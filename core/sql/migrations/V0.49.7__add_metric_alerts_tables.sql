
CREATE TABLE metric_type (
    id      INTEGER PRIMARY KEY,
    type    VARCHAR(255)
);

INSERT INTO metric_type (id, type) VALUES (1, 'resource_agent');
INSERT INTO metric_type (id, type) VALUES (2, 'network_service');

CREATE TABLE metric (
    id              INTEGER PRIMARY KEY,
    name            VARCHAR(255),
    metric_type_id  INTEGER REFERENCES metric_type (id)
);

INSERT INTO metric (id, name, metric_type_id) VALUES (1, 'CPU', 1);
INSERT INTO metric (id, name, metric_type_id) VALUES (2, 'RAM', 1);
INSERT INTO metric (id, name, metric_type_id) VALUES (3, 'DISK', 1);

INSERT INTO metric (id, name, metric_type_id) VALUES (4, 'FTP', 2);
INSERT INTO metric (id, name, metric_type_id) VALUES (5, 'SSH', 2);
INSERT INTO metric (id, name, metric_type_id) VALUES (6, 'SMTP', 2);
INSERT INTO metric (id, name, metric_type_id) VALUES (7, 'HTTP', 2);
INSERT INTO metric (id, name, metric_type_id) VALUES (8, 'IMAP', 2);
INSERT INTO metric (id, name, metric_type_id) VALUES (9, 'POP3', 2);
INSERT INTO metric (id, name, metric_type_id) VALUES (10, 'PING', 2);

CREATE TABLE vm_silenced_alert (
    id          SERIAL,
    vm_id       UUID REFERENCES virtual_machine (vm_id),
    metric_id   INTEGER REFERENCES metric (id),
    PRIMARY KEY (vm_id, metric_id)
);

