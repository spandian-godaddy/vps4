ALTER TABLE panopta_server ADD COLUMN template_id VARCHAR(255);

CREATE TABLE monitoring_pf
(
    vm_id UUID NOT NULL PRIMARY KEY REFERENCES virtual_machine(vm_id),
    monitoring  BOOLEAN NOT NULL DEFAULT FALSE
)