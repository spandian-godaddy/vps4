CREATE TABLE vm_user (
    name                VARCHAR(255)    NOT NULL,
    vm_id               BIGINT          NOT NULL    REFERENCES virtual_machine(vm_id),
    admin_enabled       BOOLEAN         NOT NULL    DEFAULT 'FALSE',
    
    PRIMARY KEY(vm_id, name)
);


CREATE OR REPLACE FUNCTION user_create(p_name TEXT 
    , p_vm_id BIGINT
    , p_admin_enabled BOOLEAN)

    RETURNS VOID AS $$
DECLARE
BEGIN
    
    INSERT INTO vm_user (vm_id, name, admin_enabled)
       VALUES (p_vm_id, p_name, p_admin_enabled);
       
END;
$$ LANGUAGE plpgsql;