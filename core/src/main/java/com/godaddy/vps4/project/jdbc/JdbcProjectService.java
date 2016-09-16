package com.godaddy.vps4.project.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;

public class JdbcProjectService implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcProjectService.class);

    private final DataSource dataSource;

    @Inject
    public JdbcProjectService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Project> getProjects(long userId, boolean active) {

        if (active) {
            return Sql.with(dataSource).exec(
                    "SELECT project_id," +
                            "project_name	," +
                            "status_id," +
                            "vhfs_sgid," +
//                            "billing_account_uid," +
                            "data_center_id," +
                            "valid_on," +
                            "valid_until" +
                            " FROM get_user_projects_active(?)",
                    Sql.listOf(this::mapProject),
                    userId);

        } else {
            return Sql.with(dataSource).exec(
                    "SELECT project_id," +
                            "project_name," +
                            "status_id," +
                            "vhfs_sgid," +
//                            "billing_account_uid," +
                            "data_center_id," +
                            "valid_on," +
                            "valid_until" +
                            " FROM get_user_projects(?)",
                    Sql.listOf(this::mapProject),
                    userId);
        }
    }

    protected Project mapProject(ResultSet rs) throws SQLException {
        return new Project(rs.getLong("project_id"),
                rs.getString("project_name"),
                rs.getString("vhfs_sgid"),
//                java.util.UUID.fromString(rs.getString("billing_account_uid")),
                rs.getInt("data_center_id"),
                rs.getTimestamp("valid_on").toInstant(),
                rs.getTimestamp("valid_until").toInstant());
    }

    @Override
    public Project getProject(long project_id) {
        return Sql.with(dataSource).exec(
                "SELECT project_id," +
                        "project_name," +
                        "status_id," +
                        "vhfs_sgid," +
//                        "billing_account_uid," +
                        "data_center_id," +
                        "valid_on," +
                        "valid_until" +
                        " FROM get_project(?)",
                        Sql.nextOrNull(this::mapProject),
                        project_id);
    }

//    @Override
//    public Project createProject(String name, long userId, UUID account, short dataCenterId) {
//        logger.info("creating service group: '{}' for user {} - account: {}, data center id: {}", name, userId, account, dataCenterId);
//        long newProjectId = Sql.with(dataSource).exec("SELECT * FROM create_service_group(?, ?, ?, ?)",
//                Sql.nextOrNull(rs -> rs.getLong(1)),
//                name, userId, account, dataCenterId);
//
//        return getProject(newProjectId);
//    }

    @Override
    public Project deleteProject(long projectId) {
        logger.info("deleting service group with project_id: {}", projectId);
        Sql.with(dataSource).exec("SELECT * FROM delete_service_group(?)",
                Sql.nextOrNull(rs -> rs.getLong(1)), projectId);

        return getProject(projectId);
    }

	@Override
	public Project createProject(String name, long userId) {
		logger.info("creating service group: '{}' for user {}", name, userId);
        long newProjectId = Sql.with(dataSource).exec("SELECT * FROM create_project(?, ?)",
                Sql.nextOrNull(rs -> rs.getLong(1)),
                name, userId);

        return getProject(newProjectId);
	}
}
