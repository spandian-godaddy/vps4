CREATE OR REPLACE FUNCTION add_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT,
    p_privilege_id INT
)
RETURNS INT AS $$
BEGIN

	IF check_privilege(p_user_id, p_sgid, p_privilege_id) < 0 THEN
	
	   IF p_sgid IS NOT NULL THEN
	       -- service group-specific privilege
	       INSERT INTO user_service_group_privilege (user_id, sgid, privilege_id)
	         VALUES (p_user_id, p_sgid, p_privilege_id);
       ELSE
           -- user-level privilege
           INSERT INTO user_privilege (user_id, privilege_id)
             VALUES (p_user_id, p_privilege_id);
       END IF;
	END IF;
    
    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;