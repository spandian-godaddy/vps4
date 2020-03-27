INSERT INTO action_type (type_id, type) VALUES (42, 'RESET_BACKUP_STORAGE_CREDS');

CREATE TABLE backup_storage (
    id          SERIAL PRIMARY KEY,
    vm_id       UUID NOT NULL REFERENCES virtual_machine (vm_id),
    ftp_server  TEXT,
    ftp_user    TEXT,
    valid_on    TIMESTAMP NOT NULL DEFAULT now_utc(),
    valid_until TIMESTAMP NOT NULL DEFAULT 'infinity'
);

CREATE INDEX backup_storage_vm_id_idx ON backup_storage (vm_id);