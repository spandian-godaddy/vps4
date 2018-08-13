SELECT '_dc_' AS dc, created::DATE, count(*) cancelledcounts
FROM vm_action
WHERE action_type_id = 21
  AND created > '2018-08-10 00:00:00.000000'
GROUP BY created::DATE
ORDER BY created::DATE DESC
LIMIT 5