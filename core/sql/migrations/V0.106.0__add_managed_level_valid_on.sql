CREATE TABLE managed_server (
    vm_id UUID UNIQUE REFERENCES virtual_machine(vm_id),
    managed_level INTEGER DEFAULT 0,
    valid_on TIMESTAMP DEFAULT 'infinity'
);

INSERT INTO managed_server (vm_id, managed_level, valid_on)
SELECT vm_id, managed_level, valid_on
FROM virtual_machine
WHERE managed_level > 0;