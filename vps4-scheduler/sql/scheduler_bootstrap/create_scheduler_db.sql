
DROP DATABASE IF EXISTS ${db.vps4.scheduler.database};
CREATE DATABASE ${db.vps4.scheduler.database};

-- create the role only if it doesn't exist
-- NOTE: this will handle initial database setup, but will
--       NOT care of the password of a user is modified in
--       the config files.  Changing a user password in
--       an existing environment is a manual step.
DO
$body$
BEGIN
  IF NOT EXISTS (
      SELECT *
      FROM   pg_catalog.pg_user
      WHERE  usename = '${db.vps4.scheduler.username}') THEN

    CREATE USER ${db.vps4.scheduler.username} WITH PASSWORD '${db.vps4.scheduler.password}';
  END IF;
END
$body$;

GRANT CONNECT ON DATABASE ${db.vps4.scheduler.database} TO ${db.vps4.scheduler.username};
GRANT CONNECT ON DATABASE ${db.vps4.scheduler.database} TO PUBLIC;
