-- Cleanup duplicate outages of same panopta outage id with same metric id
DELETE FROM vm_outage a USING vm_outage b WHERE a.id < b.id AND a.metric_id = b.metric_id AND a.panopta_outage_id = b.panopta_outage_id;

-- Prevent duplicate panopta outage id with same metric id
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS vm_outage_metric_id_panopta_outage_id_idx ON vm_outage (metric_id, panopta_outage_id);

