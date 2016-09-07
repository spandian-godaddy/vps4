
CREATE OR REPLACE FUNCTION check_any_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT
)
RETURNS INT AS $$
DECLARE
    privilege_count INT;
BEGIN
    
	IF p_sgid IS NOT NULL THEN
	    -- check for privilege within service group
	    SELECT COUNT(privilege_id) INTO privilege_count
	    FROM user_service_group_privilege
	    WHERE user_id=p_user_id 
	      AND sgid IS NOT DISTINCT FROM p_sgid
	      AND NOW() < valid_until;
    ELSE
        -- check for user privilege
        SELECT COUNT(privilege_id) INTO privilege_count
        FROM user_privilege
        WHERE user_id=p_user_id 
          AND NOW() < valid_until;
    END IF;
    
    IF privilege_count > 0 THEN
      RETURN 1;
    END IF;
    
    RETURN 0;
    
END;
$$ LANGUAGE plpgsql;
