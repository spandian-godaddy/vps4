
CREATE OR REPLACE FUNCTION check_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT,
    p_privilege_id INT
)
RETURNS INT AS $$
DECLARE
    t_privilege_id INT;
BEGIN
    
	IF p_sgid IS NOT NULL THEN
	    SELECT privilege_id INTO t_privilege_id
	    FROM user_service_group_privilege
	    WHERE user_id=p_user_id 
	      AND sgid IS NOT DISTINCT FROM p_sgid
	      AND privilege_id=p_privilege_id
	      AND NOW() < valid_until;
    ELSE
        SELECT privilege_id INTO t_privilege_id
        FROM user_privilege
        WHERE user_id=p_user_id 
          AND privilege_id=p_privilege_id
          AND NOW() < valid_until;
    END IF;
    
    IF NOT FOUND THEN
      RETURN -1;
    END IF;
    
    RETURN t_privilege_id;
    
END;
$$ LANGUAGE plpgsql;
