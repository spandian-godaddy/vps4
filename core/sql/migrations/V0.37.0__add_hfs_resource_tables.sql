CREATE TABLE hfs_vm
(
    hfs_vm_id BIGINT PRIMARY KEY,
    vm_id UUID NOT NULL REFERENCES virtual_machine (vm_id),
    orion_guid UUID NOT NULL,
    requested TIMESTAMP NOT NULL DEFAULT NOW(),
    created TIMESTAMP,
    canceled TIMESTAMP,
    destroyed TIMESTAMP
);
