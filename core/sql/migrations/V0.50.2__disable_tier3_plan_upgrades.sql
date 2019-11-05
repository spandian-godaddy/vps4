-- Disable plan upgrades to vps4 tier 30 also known as tier3 in ecomm
UPDATE plan SET enabled=false WHERE spec_id IN (SELECT spec_id FROM virtual_machine_spec WHERE tier=30);

