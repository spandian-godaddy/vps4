-- Cleanup duplicate outages of same panopta outage id with same metric id
DELETE FROM vm_outage a USING vm_outage b WHERE a.id < b.id AND a.metric_id = b.metric_id AND a.panopta_outage_id = b.panopta_outage_id;

