ALTER TABLE project ADD COLUMN vps4_user_id bigint references vps4_user(vps4_user_id);


update project set vps4_user_id = (select vps4_user_id from user_project_privilege where project.project_id = user_project_privilege.project_id and user_project_privilege.valid_until = 'infinity');
update project set vps4_user_id = (select vps4_user_id from user_project_privilege where project.project_id = user_project_privilege.project_id order by user_project_privilege.valid_on desc limit 1) where vps4_user_id is null;


create or replace function create_project(p_project_name character varying, p_owner_user_id bigint, p_sgid_prefix character varying) returns bigint
    language plpgsql
as
$$
DECLARE
    new_project_id BIGINT;
BEGIN
    -- create the project
    INSERT INTO project (project_name, vhfs_sgid, vps4_user_id)
    VALUES (p_project_name, p_sgid_prefix || currval('project_project_id_seq'), p_owner_user_id)
    RETURNING project_id
        INTO new_project_id;
    RETURN new_project_id;

END;
$$;
