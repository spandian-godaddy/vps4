
-- we can't drop the database while there are active connections,
-- so drop all the connections
DO
$do$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_database WHERE datname = trim(both from '${db.vps4.scheduler.database}')) THEN

    -- don't allow any new connections
    REVOKE CONNECT ON DATABASE ${db.vps4.scheduler.database} FROM PUBLIC;

    -- drop existing connections
    PERFORM pg_terminate_backend(pg_stat_activity.pid)
    FROM pg_stat_activity
    WHERE pg_stat_activity.datname = '${db.vps4.scheduler.database}'
          AND pid <> pg_backend_pid();

  END IF;
END
$do$;

DROP DATABASE IF EXISTS ${db.vps4.scheduler.database};