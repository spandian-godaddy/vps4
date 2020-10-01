ALTER TABLE monitoring_checkpoint RENAME TO action_checkpoint;

CREATE TABLE checkpoint (
    checkpoint_id   SERIAL      PRIMARY KEY,
    name            VARCHAR     UNIQUE NOT NULL,
    checkpoint      TIMESTAMP   NOT NULL DEFAULT now_utc()
);