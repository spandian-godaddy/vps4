-- Add the column with the foreign key constraint.
ALTER TABLE panopta_additional_fqdns ADD COLUMN id integer CONSTRAINT panopta_additional_fqdns_id_fkey REFERENCES panopta_server (id);
-- Back fill the respective values for id into panopta_additional_fqdns.
UPDATE panopta_additional_fqdns SET id = panopta_server.id FROM panopta_server WHERE panopta_additional_fqdns.server_id = panopta_server.server_id;
-- Drop the foreign key constraint for server_id in panopta_additional_fqdns first,
-- so we can remove the unique constraint in panopta_server.
ALTER TABLE panopta_additional_fqdns DROP CONSTRAINT panopta_additional_fqdns_server_id_fkey;
-- Drop the unique constraint for server_id in panopta_server.
ALTER TABLE panopta_server DROP CONSTRAINT panopta_server_server_id_key;
-- Add the new unique constraint for server_id and destroyed for thoroughness in panopta_server.
ALTER TABLE panopta_server ADD CONSTRAINT server_id_and_destroy_date_unique UNIQUE(server_id, destroyed);
-- Drop the server_id column in panopta_additional_fqdns.
-- This will require refactoring in the JdbcPanoptaDataService.java file.
ALTER TABLE panopta_additional_fqdns DROP COLUMN server_id;