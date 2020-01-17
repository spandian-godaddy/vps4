-- Panopta outage id can refer to multiple metric outages
ALTER TABLE vm_outage DROP CONSTRAINT vm_outage_panopta_outage_id_key;

CREATE INDEX CONCURRENTLY ON vm_outage(panopta_outage_id);

