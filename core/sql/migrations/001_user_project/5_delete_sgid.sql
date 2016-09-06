CREATE OR REPLACE FUNCTION delete_service_group( 
    p_sgid      BIGINT )
            
RETURNS BIGINT AS $$
BEGIN
    UPDATE service_group
       SET valid_until = NOW()
     WHERE sgid = p_sgid;

    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;