
CREATE DATABASE ${db.vps4.database};

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
      WHERE  usename = '${db.vps4.username}') THEN

      CREATE USER ${db.vps4.username} WITH PASSWORD '${db.vps4.password}';
   END IF;
END
$body$;

GRANT CONNECT ON DATABASE ${db.vps4.database} TO ${db.vps4.username};

