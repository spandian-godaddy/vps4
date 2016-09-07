CREATE OR REPLACE FUNCTION delete_project( 
    p_project_id      BIGINT )
            
RETURNS BIGINT AS $$
BEGIN
    UPDATE project
       SET valid_until = NOW()
     WHERE project_id = p_project_id;

    RETURN 1;
    
END;
$$ LANGUAGE plpgsql;