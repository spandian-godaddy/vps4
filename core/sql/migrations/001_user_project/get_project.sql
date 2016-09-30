DROP FUNCTION IF EXISTS get_project(IN p_project_id BIGINT);
CREATE OR REPLACE FUNCTION get_project(
    IN p_project_id BIGINT)
    RETURNS TABLE(project_id BIGINT,
	    project_name VARCHAR(255),
    	status_id SMALLINT,
    	vhfs_sgid VARCHAR(20),
    	data_center_id INT,
        valid_on TIMESTAMP,
        valid_until TIMESTAMP) AS $$
BEGIN

    RETURN QUERY
    SELECT
        p.project_id,
        p.project_name,
        p.status_id,
        p.vhfs_sgid,
        p.data_center_id,
        p.valid_on,
        p.valid_until
    FROM project p
    WHERE p.project_id = p_project_id;

END;
$$ LANGUAGE plpgsql;