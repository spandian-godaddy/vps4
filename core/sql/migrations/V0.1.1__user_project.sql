
CREATE TABLE vps4_user (
    vps4_user_id BIGSERIAL PRIMARY KEY,
    shopper_id   VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE project (
    project_id     BIGSERIAL PRIMARY KEY,
    project_name   VARCHAR(255) NOT NULL,
    data_center_id INT NOT NULL REFERENCES data_center (data_center_id),
    vhfs_sgid      varchar(20),
    valid_on       TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until    TIMESTAMP DEFAULT 'infinity',
    status_id      SMALLINT          -- ACTIVE|SUSPENDED|INACTIVE
);

-- TODO this should be broken out into a test dataset
--      it shouldn't be included in the prod database build
INSERT INTO vps4_user (shopper_id) VALUES ('TestUser');
INSERT INTO vps4_user (shopper_id) VALUES ('SomeUser');



CREATE OR REPLACE FUNCTION create_project(p_project_name  VARCHAR(255), 
                                          p_owner_user_id BIGINT,
                                          p_data_center_id INT)
    RETURNS BIGINT AS $$
DECLARE
    new_project_id BIGINT;
BEGIN
    -- create the project
    INSERT INTO project (project_name, vhfs_sgid, data_center_id)
    VALUES (p_project_name, 'vps4-' || currval('project_project_id_seq'), p_data_center_id)
    RETURNING project_id
        INTO new_project_id;

    -- add the owner
    INSERT INTO user_project_privilege (vps4_user_id, project_id, privilege_id) VALUES (p_owner_user_id, new_project_id, 1);

    -- pg_notify('project_created', project_id_payload)

    RETURN new_project_id;

END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION delete_project( 
    p_project_id      BIGINT )
            
RETURNS BIGINT AS $$
BEGIN
    UPDATE project
       SET valid_until = NOW()
     WHERE project_id = p_project_id;

    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;


DROP FUNCTION IF EXISTS get_project(IN p_project_id BIGINT);
CREATE OR REPLACE FUNCTION get_project(
    IN p_project_id BIGINT)
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
    WHERE p.project_id = p_project_id;

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
    WHERE upp.vps4_user_id = p_user_id AND p.valid_until > NOW();

END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION get_user_projects(p_user_id vps4_user.vps4_user_id%TYPE)
    RETURNS TABLE(project_id BIGINT,
    project_name VARCHAR(255),
    status_id SMALLINT,
    vhfs_sgid VARCHAR(20),
    data_center_id SMALLINT,
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
    WHERE upp.vps4_user_id = p_user_id;

END
$$ LANGUAGE plpgsql;
