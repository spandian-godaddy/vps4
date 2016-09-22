
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


