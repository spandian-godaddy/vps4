
DROP TABLE IF EXISTS service_group, mcs_user, user_service_group CASCADE;

CREATE TABLE vps4_user (
    vps4_user_id BIGSERIAL PRIMARY KEY,
    shopper_id VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE service_group (
    sgid BIGSERIAL PRIMARY KEY,
    service_group_name VARCHAR(255) NOT NULL,
    vhfs_sgid varchar(20),
    valid_on TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_until TIMESTAMP DEFAULT 'infinity',
    status_id SMALLINT          -- ACTIVE|SUSPENDED|INACTIVE
);

CREATE TABLE user_service_group (
    vps4_user_id BIGINT NOT NULL REFERENCES vps4_user(vps4_user_id),
    sgid BIGINT NOT NULL REFERENCES service_group(sgid),
    
    owner_priv          BOOLEAN DEFAULT FALSE,
    create_delete_priv  BOOLEAN DEFAULT FALSE,
    manage_priv         BOOLEAN DEFAULT FALSE,
    configure_priv      BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (vps4_user_id, sgid)
);