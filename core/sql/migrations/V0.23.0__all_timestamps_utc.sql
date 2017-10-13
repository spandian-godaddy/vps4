CREATE OR REPLACE FUNCTION public.now_utc(
	)
    RETURNS timestamp without time zone
    LANGUAGE 'sql'

AS $BODY$

  select now() at time zone 'utc';

$BODY$;

-- project
ALTER TABLE project ALTER COLUMN valid_on SET DEFAULT now_utc();
update project set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update project set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

CREATE OR REPLACE FUNCTION delete_project( 
    p_project_id      BIGINT )
            
RETURNS BIGINT AS $$
BEGIN
    UPDATE project
       SET valid_until = now_utc()
     WHERE project_id = p_project_id;

    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_user_projects_active(p_user_id vps4_user.vps4_user_id%TYPE)
    RETURNS TABLE(project_id BIGINT,
    project_name VARCHAR(255),
    status_id SMALLINT,
    vhfs_sgid VARCHAR(20),
    data_center_id INT,
    valid_on TIMESTAMP,
    valid_until TIMESTAMP) AS $$
BEGIN

    RETURN QUERY
    SELECT
        p.project_id,
        p.project_name,
        p.status_id,
        p.vhfs_sgid,
        p.data_center_id,
        p.valid_on,
        p.valid_until
    FROM project p
        INNER JOIN user_project_privilege upp ON p.project_id = upp.project_id
    WHERE upp.vps4_user_id = p_user_id AND p.valid_until > now_utc();

END
$$ LANGUAGE plpgsql;

-- user
ALTER TABLE user_privilege ALTER COLUMN valid_on SET DEFAULT now_utc();
update user_privilege set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update user_privilege set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

ALTER TABLE user_project_privilege ALTER COLUMN valid_on SET DEFAULT now_utc();
update user_project_privilege set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update user_project_privilege set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

CREATE OR REPLACE FUNCTION check_any_privilege(
    p_user_id      BIGINT,
    p_project_id         BIGINT
)
RETURNS INT AS $$
DECLARE
    privilege_count INT;
BEGIN
    
    IF p_project_id IS NOT NULL THEN
        -- check for privilege within service group
        SELECT COUNT(privilege_id) INTO privilege_count
        FROM user_project_privilege
        WHERE vps4_user_id=p_user_id 
          AND project_id IS NOT DISTINCT FROM p_project_id
          AND now_utc() < valid_until;
    ELSE
        -- check for user privilege
        SELECT COUNT(privilege_id) INTO privilege_count
        FROM user_privilege
        WHERE vps4_user_id=p_user_id 
          AND now_utc() < valid_until;
    END IF;
    
    IF privilege_count > 0 THEN
      RETURN 1;
    END IF;
    
    RETURN 0;
    
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION check_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT,
    p_privilege_id INT
)
RETURNS INT AS $$
DECLARE
    t_privilege_id INT;
BEGIN
    
    IF p_sgid IS NOT NULL THEN
        SELECT privilege_id INTO t_privilege_id
        FROM user_service_group_privilege
        WHERE user_id=p_user_id 
          AND sgid IS NOT DISTINCT FROM p_sgid
          AND privilege_id=p_privilege_id
          AND now_utc() < valid_until;
    ELSE
        SELECT privilege_id INTO t_privilege_id
        FROM user_privilege
        WHERE user_id=p_user_id 
          AND privilege_id=p_privilege_id
          AND now_utc() < valid_until;
    END IF;
    
    IF NOT FOUND THEN
      RETURN -1;
    END IF;
    
    RETURN t_privilege_id;
    
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION remove_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT,
    p_privilege_id INT
)
RETURNS INT AS $$
BEGIN

    IF p_sgid IS NOT NULL THEN
        UPDATE user_service_group_privilege
        SET
           valid_until=now_utc()
        WHERE
           user_id=p_user_id
           AND sgid IS NOT DISTINCT FROM p_sgid
           AND privilege_id=p_privilege_id
           AND valid_until='infinity';
    ELSE
        UPDATE user_privilege
        SET
           valid_until=now_utc()
        WHERE
           user_id=p_user_id
           AND privilege_id=p_privilege_id
           AND valid_until='infinity';
    END IF;
    
    IF NOT FOUND THEN
        RETURN 0;
    END IF;
       
    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;

--image
ALTER TABLE image ALTER COLUMN valid_on SET DEFAULT now_utc();
update image set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update image set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

--snapshot
ALTER TABLE snapshot ALTER COLUMN created_at SET DEFAULT now_utc();
update snapshot set created_at = (created_at AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update snapshot set modified_at = (modified_at AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

ALTER TABLE snapshot_action ALTER COLUMN created SET DEFAULT now_utc();
update snapshot_action set created = (created AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

--virtual machine
ALTER TABLE virtual_machine_spec ALTER COLUMN valid_on SET DEFAULT now_utc();
update virtual_machine_spec set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update virtual_machine_spec set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

ALTER TABLE virtual_machine ALTER COLUMN valid_on SET DEFAULT now_utc();
update virtual_machine set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update virtual_machine set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

ALTER TABLE vm_action ALTER COLUMN created SET DEFAULT now_utc();
update vm_action set created = (created AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

--ip address
ALTER TABLE ip_address ALTER COLUMN valid_on SET DEFAULT now_utc();
update ip_address set valid_on = (valid_on AT TIME ZONE 'MST') AT TIME ZONE 'UTC';
update ip_address set valid_until = (valid_until AT TIME ZONE 'MST') AT TIME ZONE 'UTC';

CREATE OR REPLACE FUNCTION ip_address_delete(
    p_ip_address_id BIGINT
    )

    RETURNS VOID AS $$
BEGIN
    
     UPDATE ip_address SET valid_until = now_utc() WHERE ip_address_id = p_ip_address_id;

END;
$$ LANGUAGE plpgsql;