CREATE OR REPLACE FUNCTION remove_privilege(
    p_user_id      BIGINT,
    p_sgid         BIGINT,
    p_privilege_id INT
)
RETURNS INT AS $$
BEGIN

	IF p_sgid IS NOT NULL THEN
		UPDATE user_service_group_privilege
		SET
		   valid_until=NOW()
		WHERE
		   user_id=p_user_id
		   AND sgid IS NOT DISTINCT FROM p_sgid
		   AND privilege_id=p_privilege_id
		   AND valid_until='infinity';
    ELSE
        UPDATE user_privilege
        SET
           valid_until=NOW()
        WHERE
           user_id=p_user_id
           AND privilege_id=p_privilege_id
           AND valid_until='infinity';
    END IF;
    
    IF NOT FOUND THEN
        RETURN 0;
    END IF;
	   
    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;