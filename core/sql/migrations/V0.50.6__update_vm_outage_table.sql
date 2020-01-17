-- Panopta outage id can refer to multiple metric outages
ALTER TABLE vm_outage DROP CONSTRAINT IF EXISTS vm_outage_panopta_outage_id_key;

CREATE INDEX ON vm_outage(panopta_outage_id);

