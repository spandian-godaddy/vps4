
DROP TABLE IF EXISTS project, vps4_user, user_project CASCADE;

CREATE TABLE vps4_user (
    vps4_user_id BIGSERIAL PRIMARY KEY,
    shopper_id   VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE project (
    project_id   BIGSERIAL PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    vhfs_sgid    varchar(20),
    valid_on     TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until  TIMESTAMP DEFAULT 'infinity',
    status_id    SMALLINT          -- ACTIVE|SUSPENDED|INACTIVE
);

CREATE TABLE user_project (
    vps4_user_id 		BIGINT NOT NULL REFERENCES vps4_user(vps4_user_id),
    project_id 			BIGINT NOT NULL REFERENCES project(project_id),
    
    owner_priv          BOOLEAN DEFAULT FALSE,
    create_delete_priv  BOOLEAN DEFAULT FALSE,
    manage_priv         BOOLEAN DEFAULT FALSE,
    configure_priv      BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (vps4_user_id, project_id)
);