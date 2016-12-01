
CREATE TABLE privilege (
    privilege_id    BIGINT PRIMARY KEY,
    privilege_tx    VARCHAR(255) NOT NULL,
    privilege_desc  TEXT NOT NULL
);

-- the 'global' privileges a user possesses (outside the context of a particular project)
CREATE TABLE user_privilege (
    vps4_user_id         BIGINT NOT NULL REFERENCES vps4_user(vps4_user_id),
    privilege_id    BIGINT NOT NULL REFERENCES privilege(privilege_id),
    
    valid_on        TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until     TIMESTAMP NOT NULL DEFAULT 'infinity',
  
    PRIMARY KEY (vps4_user_id, privilege_id, valid_until)
);

CREATE TABLE user_project_privilege (
    vps4_user_id         BIGINT NOT NULL REFERENCES vps4_user(vps4_user_id),
    privilege_id    BIGINT NOT NULL REFERENCES privilege(privilege_id),
    project_id      BIGINT NOT NULL REFERENCES project(project_id),
    
    valid_on        TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until     TIMESTAMP NOT NULL DEFAULT 'infinity',
  
    PRIMARY KEY (vps4_user_id, project_id, privilege_id, valid_until)
);


INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (1, 'OWNER',          '');
INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (2, 'CREATE',         '');
INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (3, 'DELETE',         '');
INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (4, 'MANAGE',         '');
INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (5, 'CONFIGURE',      '');


INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (6, 'MANAGE_USERS',        'Create, manage, and remove/ban users');
INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (7, 'VIEW_USER_SGIDS',     'Read-Only access to any service group of any user');
INSERT INTO privilege(privilege_id, privilege_tx, privilege_desc) VALUES (8, 'MANAGE_USER_SGIDS',   'Management access to any service group of any user');






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
          AND NOW() < valid_until;
    ELSE
        -- check for user privilege
        SELECT COUNT(privilege_id) INTO privilege_count
        FROM user_privilege
        WHERE vps4_user_id=p_user_id 
          AND NOW() < valid_until;
    END IF;
    
    IF privilege_count > 0 THEN
      RETURN 1;
    END IF;
    
    RETURN 0;
    
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT,
    p_privilege_id INT
)
RETURNS INT AS $$
BEGIN

    IF check_privilege(p_user_id, p_sgid, p_privilege_id) < 0 THEN
    
       IF p_sgid IS NOT NULL THEN
           -- service group-specific privilege
           INSERT INTO user_service_group_privilege (user_id, sgid, privilege_id)
             VALUES (p_user_id, p_sgid, p_privilege_id);
       ELSE
           -- user-level privilege
           INSERT INTO user_privilege (user_id, privilege_id)
             VALUES (p_user_id, p_privilege_id);
       END IF;
    END IF;
    
    RETURN 1;
    
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
          AND NOW() < valid_until;
    ELSE
        SELECT privilege_id INTO t_privilege_id
        FROM user_privilege
        WHERE user_id=p_user_id 
          AND privilege_id=p_privilege_id
          AND NOW() < valid_until;
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
           valid_until=NOW()
        WHERE
           user_id=p_user_id
           AND sgid IS NOT DISTINCT FROM p_sgid
           AND privilege_id=p_privilege_id
           AND valid_until='infinity';
    ELSE
        UPDATE user_privilege
        SET
           valid_until=NOW()
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

