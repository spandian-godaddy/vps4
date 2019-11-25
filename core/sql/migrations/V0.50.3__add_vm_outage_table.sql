CREATE TABLE vm_outage (
    id          SERIAL PRIMARY KEY,
    vm_id       UUID NOT NULL REFERENCES virtual_machine (vm_id),
    metric_id   INTEGER NOT NULL REFERENCES metric (id),
    started     TIMESTAMP NOT NULL DEFAULT now_utc(),
    ended       TIMESTAMP,
    reason      TEXT,
    panopta_outage_id   BIGINT NOT NULL UNIQUE
);

CREATE INDEX panopta_alert_vm_id_idx ON vm_outage (vm_id);

