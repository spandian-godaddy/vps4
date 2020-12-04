DO $$
DECLARE
    plan_spec_id SMALLINT;
BEGIN

    SELECT DISTINCT * FROM (
        SELECT vms.spec_id
            FROM virtual_machine_spec vms
            JOIN server_type st ON vms.server_type_id = st.server_type_id
        WHERE vms.tier = 5 AND st.platform = 'OPENSTACK' AND vms.valid_until = 'infinity'
    ) q INTO plan_spec_id;

    INSERT INTO plan (pfid, package_id, term_months, os_type_id, spec_id, control_panel_id, enabled) VALUES
        (1306133, 'vps4_self_managed_lin_tier5_001mo', 1, 1, plan_spec_id, 0, 't'),
        (1306137, 'vps4_self_managed_lin_tier5_003mo', 3, 1, plan_spec_id, 0, 't'),
        (1306139, 'vps4_self_managed_lin_tier5_006mo', 6, 1, plan_spec_id, 0, 't'),
        (1306135, 'vps4_self_managed_lin_tier5_012mo', 12, 1, plan_spec_id, 0, 't'),
        (1306141, 'vps4_self_managed_lin_tier5_024mo', 24, 1, plan_spec_id, 0, 't'),
        (1306143, 'vps4_self_managed_lin_tier5_036mo', 36, 1, plan_spec_id, 0, 't'),
        (1306145, 'vps4_self_managed_lin_tier5_048mo', 48, 1, plan_spec_id, 0, 't'),
        (1306147, 'vps4_self_managed_lin_tier5_060mo', 60, 1, plan_spec_id, 0, 't');

    UPDATE plan SET term_months = 3 WHERE package_id IN (
        'vps4_managed_lin_cpanel_tier2_003mo',
        'vps4_managed_lin_plesk_tier2_003mo',
        'vps4_managed_win_plesk_tier2_003mo',
        'vps4_managed_lin_cpanel_tier1_003mo',
        'vps4_managed_lin_plesk_tier1_003mo',
        'vps4_managed_win_plesk_tier1_003mo',
        'vps4_managed_lin_cpanel_tier3_003mo',
        'vps4_managed_lin_plesk_tier3_003mo',
        'vps4_managed_lin_cpanel_tier4_003mo',
        'vps4_managed_lin_plesk_tier4_003mo',
        'vps4_managed_high_mem_lin_cpanel_tier1_003mo',
        'vps4_managed_high_mem_lin_plesk_tier1_003mo',
        'vps4_managed_high_mem_win_plesk_tier1_003mo',
        'vps4_managed_high_mem_lin_cpanel_tier4_003mo',
        'vps4_managed_high_mem_lin_plesk_tier4_003mo',
        'vps4_managed_high_mem_win_plesk_tier4_003mo',
        'vps4_managed_high_mem_lin_cpanel_tier2_003mo',
        'vps4_managed_win_plesk_tier3_003mo',
        'vps4_managed_high_mem_lin_plesk_tier2_003mo',
        'vps4_managed_high_mem_win_plesk_tier2_003mo',
        'vps4_managed_win_plesk_tier4_003mo'
    );

END $$