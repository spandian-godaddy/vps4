-- Flyway (non-trasactional migration)
-- Prevent duplicate panopta outage id with same metric id
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS vm_outage_metric_id_panopta_outage_id_idx ON vm_outage (metric_id, panopta_outage_id);

