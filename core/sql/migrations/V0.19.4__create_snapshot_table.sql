CREATE TABLE snapshot (
  id            UUID      PRIMARY KEY,
  image_id      TEXT,
  project_id    BIGINT    NOT NULL    REFERENCES project(project_id),
  snapshot_id   BIGINT,
  vm_id         UUID      NOT NULL    REFERENCES virtual_machine(vm_id),
  name          TEXT      NOT NULL
);
