package com.godaddy.vps4.project.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.jdbc.Sql;

import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.util.TimestampUtils;

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
                    "SELECT "
                    + "p.project_id, "
                    + "p.project_name, "
                    + "p.status_id, "
                    + "p.vhfs_sgid, "
                    + "p.valid_on, "
                    + "p.valid_until "
                    + "FROM project p "
                    + "INNER JOIN user_project_privilege upp ON p.project_id = upp.project_id "
                    + "WHERE upp.vps4_user_id = ? AND p.valid_until > now_utc()",
                    Sql.listOf(this::mapProject),
                    userId);

        }
        else {
            return Sql.with(dataSource).exec(
                    "SELECT "
                    + "p.project_id, "
                    + "p.project_name, "
                    + "p.status_id, "
                    + "p.vhfs_sgid, "
                    + "p.valid_on, "
                    + "p.valid_until "
                    + "FROM project p "
                    + "INNER JOIN user_project_privilege upp ON p.project_id = upp.project_id "
                    + "WHERE upp.vps4_user_id = ?",
                    Sql.listOf(this::mapProject),
                    userId);
        }
    }

    protected Project mapProject(ResultSet rs) throws SQLException {
        return new Project(rs.getLong("project_id"),
                rs.getString("project_name"),
                rs.getString("vhfs_sgid"),
                rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant(),
                rs.getTimestamp("valid_until", TimestampUtils.utcCalendar).toInstant());
    }

    @Override
    public Project getProject(long project_id) {
        return Sql.with(dataSource).exec(
                "SELECT project_id," +
                        "project_name," +
                        "status_id," +
                        "vhfs_sgid," +
                        "valid_on," +
                        "valid_until" +
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
    public Project createProject(String name, long userId, String sgidPrefix) {
        logger.info("creating project: '{}' for user {}", name, userId);
        long newProjectId = Sql.with(dataSource).exec("SELECT * FROM create_project(?, ?, ?)",
                Sql.nextOrNull(rs -> rs.getLong(1)),
                name, userId, sgidPrefix);

        return getProject(newProjectId);
    }
}
