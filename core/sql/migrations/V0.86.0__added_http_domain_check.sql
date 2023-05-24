UPDATE metric SET name = 'HTTPS_DOMAIN' where name = 'HTTPS';

INSERT INTO metric (id, name, metric_group_id) VALUES (12, 'HTTP_DOMAIN', 2);
INSERT INTO metric_type (metric_type_id, metric_id, os_type_id) VALUES
    (51,   (SELECT id FROM metric WHERE name = 'HTTP_DOMAIN'), 1),
    (51,   (SELECT id FROM metric WHERE name = 'HTTP_DOMAIN'), 2);
