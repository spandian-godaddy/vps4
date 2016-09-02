
CREATE DATABASE ${db.vps4.database};

DROP ROLE IF EXISTS ${db.vps4.username};

CREATE USER ${db.vps4.username} WITH PASSWORD '${db.vps4.password}';

GRANT CONNECT ON DATABASE ${db.vps4.database} TO ${db.vps4.username};

