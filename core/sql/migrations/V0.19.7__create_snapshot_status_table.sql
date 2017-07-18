ALTER TABLE snapshot RENAME COLUMN  image_id    TO hfs_image_id;
ALTER TABLE snapshot RENAME COLUMN  snapshot_id TO hfs_snapshot_id;

CREATE TABLE snapshot_status (
  status_id   INTEGER       PRIMARY KEY,
  status      VARCHAR(255)
);

INSERT INTO snapshot_status(status_id, status) VALUES (1, 'NEW');
INSERT INTO snapshot_status(status_id, status) VALUES (2, 'IN_PROGRESS');
INSERT INTO snapshot_status(status_id, status) VALUES (3, 'COMPLETE');
INSERT INTO snapshot_status(status_id, status) VALUES (4, 'ERROR');
INSERT INTO snapshot_status(status_id, status) VALUES (5, 'DESTROYED');

ALTER TABLE snapshot
  ADD COLUMN status       INT         NOT NULL  DEFAULT 1      REFERENCES snapshot_status(status_id),
  ADD COLUMN created_at    TIMESTAMP   NOT NULL  DEFAULT NOW(),
  ADD COLUMN modified_at   TIMESTAMP;
