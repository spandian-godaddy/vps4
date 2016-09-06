DROP FUNCTION IF EXISTS create_service_group(p_service_group_name varchar, p_owner_user_id int8);

CREATE OR REPLACE FUNCTION create_service_group(p_service_group_name  VARCHAR(255), 
												p_owner_user_id BIGINT,
                                                p_data_center_id SMALLINT)
    RETURNS BIGINT AS $$
DECLARE
    new_sgid          BIGINT;
BEGIN
    -- create the SGID
    INSERT INTO service_group (service_group_name, vhfs_sgid, data_center_id)
    VALUES (p_service_group_name, 'mcs-' || currval('service_group_sgid_seq'), p_data_center_id)
    RETURNING sgid
        INTO new_sgid;

    -- add the owner
    INSERT INTO user_service_group_privilege (user_id, sgid, privilege_id) VALUES (p_owner_user_id, new_sgid, 1);

    -- pg_notify('service_group_created', sgid_payload)

    RETURN new_sgid;

END;
$$ LANGUAGE plpgsql;