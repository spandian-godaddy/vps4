
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

CREATE TABLE user_project (
    vps4_user_id 		BIGINT NOT NULL REFERENCES vps4_user(vps4_user_id),
    project_id 			BIGINT NOT NULL REFERENCES project(project_id),
    
    PRIMARY KEY (vps4_user_id, project_id)
);