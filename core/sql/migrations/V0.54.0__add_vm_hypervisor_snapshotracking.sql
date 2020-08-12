-- table to track hypervisor info for VM with in-progress snapshot
CREATE TABLE vm_hypervisor_snapshottracking (
    id              SERIAL PRIMARY KEY,
    vm_id     UUID NOT NULL REFERENCES virtual_machine(vm_id),
    hypervisor      VARCHAR(255) NOT NULL UNIQUE,
    created         TIMESTAMP NOT NULL DEFAULT now_utc()
);