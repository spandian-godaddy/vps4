DROP FUNCTION IF EXISTS create_project(p_project_name varchar, p_owner_user_id int8);

CREATE OR REPLACE FUNCTION create_project(p_project_name  VARCHAR(255), 
										  p_owner_user_id BIGINT)
    RETURNS BIGINT AS $$
DECLARE
    new_project_id BIGINT;
BEGIN
    -- create the project
    INSERT INTO project (project_name, vhfs_sgid)
    VALUES (p_project_name, 'vps4-' || currval('project_project_id_seq'))
    RETURNING project_id
        INTO new_project_id;

    -- add the owner
    INSERT INTO user_project_privilege (vps4_user_id, project_id, privilege_id) VALUES (p_owner_user_id, new_project_id, 1);

    -- pg_notify('project_created', project_id_payload)

    RETURN new_project_id;

END;
$$ LANGUAGE plpgsql;