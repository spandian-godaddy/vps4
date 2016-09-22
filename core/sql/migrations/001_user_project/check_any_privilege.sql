
CREATE OR REPLACE FUNCTION check_any_privilege(
    p_user_id      BIGINT,
    p_project_id         BIGINT
)
RETURNS INT AS $$
DECLARE
    privilege_count INT;
BEGIN
    
	IF p_project_id IS NOT NULL THEN
	    -- check for privilege within service group
	    SELECT COUNT(privilege_id) INTO privilege_count
	    FROM user_project_privilege
	    WHERE vps4_user_id=p_user_id 
	      AND project_id IS NOT DISTINCT FROM p_project_id
	      AND NOW() < valid_until;
    ELSE
        -- check for user privilege
        SELECT COUNT(privilege_id) INTO privilege_count
        FROM user_privilege
        WHERE vps4_user_id=p_user_id 
          AND NOW() < valid_until;
    END IF;
    
    IF privilege_count > 0 THEN
      RETURN 1;
    END IF;
    
    RETURN 0;
    
END;
$$ LANGUAGE plpgsql;
