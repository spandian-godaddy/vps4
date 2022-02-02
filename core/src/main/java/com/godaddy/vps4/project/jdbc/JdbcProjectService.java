package com.godaddy.vps4.project.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.util.TimestampUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcProjectService implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcProjectService.class);

    private final DataSource dataSource;

    @Inject
    public JdbcProjectService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Project> getProjects(long userId, boolean active) {

        String whereClause = "WHERE p.vps4_user_id = ?";
        if (active) whereClause += " AND p.valid_until > now_utc()";

        return Sql.with(dataSource).exec(
                "SELECT "
                + "p.project_id, "
                + "p.project_name, "
                + "p.status_id, "
                + "p.vhfs_sgid, "
                + "p.valid_on, "
                + "p.valid_until, "
                + "p.vps4_user_id "
                + "FROM project p "
                + whereClause,
                Sql.listOf(this::mapProject),
                userId);
    }

    protected Project mapProject(ResultSet rs) throws SQLException {
        return new Project(rs.getLong("project_id"),
                rs.getString("project_name"),
                rs.getString("vhfs_sgid"),
                rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant(),
                rs.getTimestamp("valid_until", TimestampUtils.utcCalendar).toInstant(),
                rs.getLong("vps4_user_id"));
    }

    @Override
    public Project getProject(long project_id) {
        return Sql.with(dataSource).exec(
                "SELECT project_id," +
                        "project_name," +
                        "status_id," +
                        "vhfs_sgid," +
                        "valid_on," +
                        "valid_until, " +
                        "vps4_user_id " +
                        " FROM project where project_id = ?",
                Sql.nextOrNull(this::mapProject),
                project_id);
    }

    // @Override
    // public Project createProject(String name, long userId, UUID account, short dataCenterId) {
    // logger.info("creating service group: '{}' for user {} - account: {}, data center id: {}", name, userId, account, dataCenterId);
    // long newProjectId = Sql.with(dataSource).exec("SELECT * FROM create_service_group(?, ?, ?, ?)",
    // Sql.nextOrNull(rs -> rs.getLong(1)),
    // name, userId, account, dataCenterId);
    //
    // return getProject(newProjectId);
    // }

    @Override
    public Project deleteProject(long projectId) {
        logger.info("deleting service group with project_id: {}", projectId);
        Sql.with(dataSource).exec("SELECT * FROM delete_project(?)",
                Sql.nextOrNull(rs -> rs.getLong(1)), projectId);

        return getProject(projectId);
    }

    @Override
    public void updateProjectUser(long projectId, long id) {
        logger.info("updating project {} with new userid {}", projectId, id);
        Sql.with(dataSource).exec("UPDATE project SET vps4_user_id = ? where project_id = ?", null, id, projectId);
    }

    @Override
    public Project createProject(String name, long userId, String sgidPrefix) {
        logger.info("creating project: '{}' for user {}", name, userId);
        long newProjectId = Sql.with(dataSource).exec("SELECT * FROM create_project(?, ?, ?)",
                Sql.nextOrNull(rs -> rs.getLong(1)),
                name, userId, sgidPrefix);

        return getProject(newProjectId);
    }

    @Override
    public Project createProjectAndPrivilegeWithSgid(String name, long userId, String sgid) {
        logger.info("creating project: '{}' for user {} with sgid {}", name, userId, sgid);
        long newProjectId = Sql.with(dataSource).exec("INSERT INTO project (project_name, vhfs_sgid, vps4_user_id) VALUES (?, ?, ?) " +
                        "RETURNING project_id", Sql.nextOrNull(rs -> rs.getLong(1)), name, sgid, userId);
        return getProject(newProjectId);
    }
}
