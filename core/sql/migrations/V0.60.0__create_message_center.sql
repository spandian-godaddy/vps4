CREATE TABLE notification_type (
    type_id   INTEGER PRIMARY KEY,
    type      VARCHAR(255)    UNIQUE
);


CREATE TABLE notification (
    notification_id         UUID      PRIMARY KEY
    , type_id               INTEGER      NOT NULL    REFERENCES notification_type(type_id)
    , support_only          BOOLEAN         NOT NULL    DEFAULT 'FALSE'
    , dismissible           BOOLEAN         NOT NULL    DEFAULT 'FALSE'
    , valid_on       TIMESTAMP   NOT NULL    DEFAULT NOW_UTC()
    , valid_until       TIMESTAMP   NOT NULL    DEFAULT 'infinity'

);


CREATE TABLE notification_extended_details (
     notification_id        UUID      PRIMARY KEY REFERENCES notification(notification_id)
    , start_time       TIMESTAMP   NOT NULL    DEFAULT NOW_UTC()
    , end_time         TIMESTAMP   NOT NULL    DEFAULT 'infinity'
);

CREATE TABLE notification_filter_type (
    filter_type_id   INTEGER PRIMARY KEY,
    filter_type      VARCHAR(255)    UNIQUE
);


CREATE TABLE notification_filter (
    notification_filter_id  SERIAL         PRIMARY KEY
    , notification_id        UUID         REFERENCES notification(notification_id)
    , filter_type_id              INTEGER         REFERENCES notification_filter_type(filter_type_id)
    , filter_value           VARCHAR(255)[]
);

INSERT into notification_type (type_id, type) VALUES (1,'PATCHING'),
                                                     (2, 'MAINTENANCE'),
                                                     (3, 'NEW_FEATURE_OPCACHE');

INSERT INTO notification_filter_type (filter_type_id, filter_type)
VALUES (1, 'IMAGE_ID'), (2, 'RESELLER_ID'), (3, 'HYPERVISOR_HOSTNAME'),
       (4, 'TIER'), (5, 'PLATFORM_ID'), (6, 'VM_ID');

