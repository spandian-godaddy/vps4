CREATE TABLE monitoring_checkpoint (
    action_type_id  INTEGER     NOT NULL REFERENCES action_type(type_id) PRIMARY KEY,
    checkpoint      TIMESTAMP   NOT NULL DEFAULT now_utc()
);