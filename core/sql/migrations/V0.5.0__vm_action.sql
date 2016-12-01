
CREATE TABLE action_status(
    status_id   INTEGER         PRIMARY KEY
    , status    VARCHAR(255)
);

CREATE TABLE action_type(
    type_id     INTEGER         PRIMARY KEY
    , type      VARCHAR(255)    UNIQUE
);

CREATE TABLE vm_action (
    id                      SERIAL      PRIMARY KEY
    , vm_id                 BIGINT                  REFERENCES virtual_machine(vm_id)
    , action_type_id        BIGINT      NOT NULL    REFERENCES action_type(type_id) DEFAULT 1
    , vps4_user_id          BIGINT      NOT NULL    REFERENCES vps4_user(vps4_user_id)
    , command_id            UUID        
    , request               JSON
    , state                 JSON
    , response              JSON
    , status_id             BIGINT      NOT NULL    REFERENCES action_status(status_id) DEFAULT 1
    , created               TIMESTAMP   NOT NULL    DEFAULT NOW()
    , note                  VARCHAR(255)
);


INSERT INTO action_status(status_id, status) VALUES (1, 'NEW');
INSERT INTO action_status(status_id, status) VALUES (2, 'IN_PROGRESS');
INSERT INTO action_status(status_id, status) VALUES (3, 'COMPLETE');
INSERT INTO action_status(status_id, status) VALUES (4, 'ERROR');
INSERT INTO action_status(status_id, status) VALUES (5, 'CANCELLED');
INSERT INTO action_status(status_id, status) VALUES (6, 'INVALID');

INSERT INTO action_type(type_id, type) VALUES (1, 'CREATE_VM');
INSERT INTO action_type(type_id, type) VALUES (2, 'RESTART_VM');
INSERT INTO action_type(type_id, type) VALUES (3, 'ENABLE_ADMIN_ACCESS');
INSERT INTO action_type(type_id, type) VALUES (4, 'DISABLE_ADMIN_ACCESS');
INSERT INTO action_type(type_id, type) VALUES (5, 'START_VM');
INSERT INTO action_type(type_id, type) VALUES (6, 'STOP_VM');
INSERT INTO action_type(type_id, type) VALUES (7, 'DESTROY_VM');
INSERT INTO action_type(type_id, type) VALUES (8, 'SET_PASSWORD');

