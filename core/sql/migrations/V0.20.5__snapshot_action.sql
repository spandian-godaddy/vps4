CREATE TABLE snapshot_action (
    id                      SERIAL      PRIMARY KEY
    , snapshot_id           UUID        NOT NULL    REFERENCES snapshot(id)
    , action_type_id        BIGINT      NOT NULL    REFERENCES action_type(type_id)
    , vps4_user_id          BIGINT      NOT NULL    REFERENCES vps4_user(vps4_user_id)
    , command_id            UUID
    , request               JSON
    , state                 JSON
    , response              JSON
    , status_id             BIGINT      NOT NULL    REFERENCES action_status(status_id) DEFAULT 1
    , created               TIMESTAMP   NOT NULL    DEFAULT NOW()
    , note                  VARCHAR(255)
);

CREATE INDEX snapshot_action_snapshot_id on snapshot_action (snapshot_id);

INSERT INTO action_type(type_id, type) VALUES (13, 'CREATE_SNAPSHOT');
INSERT INTO action_type(type_id, type) VALUES (14, 'DESTROY_SNAPSHOT');
