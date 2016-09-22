
CREATE OR REPLACE FUNCTION get_user_projects_active(p_user_id vps4_user.vps4_user_id%TYPE)
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
    	INNER JOIN user_project_privilege upp ON p.project_id = upp.project_id
    WHERE upp.vps4_user_id = p_user_id AND p.valid_until > NOW();

END
$$ LANGUAGE plpgsql;
