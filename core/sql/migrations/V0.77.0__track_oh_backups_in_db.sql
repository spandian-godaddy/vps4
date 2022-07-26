CREATE TABLE oh_backup (
    backup_id       SERIAL PRIMARY KEY,
    oh_backup_id    UUID,
    vm_id           UUID REFERENCES virtual_machine (vm_id),
    name            TEXT NOT NULL,
    created         TIMESTAMP NOT NULL DEFAULT now_utc(),
    destroyed       TIMESTAMP NOT NULL DEFAULT 'infinity'
);
