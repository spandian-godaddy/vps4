package com.godaddy.vps4.security.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;

import com.godaddy.vps4.project.ProjectPrivilege;
import com.godaddy.vps4.project.UserProjectPrivilege;
import com.godaddy.vps4.security.Privilege;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.util.TimestampUtils;


public class JdbcPrivilegeService implements PrivilegeService {

    private final DataSource dataSource;

    @Inject
    public JdbcPrivilegeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void requirePrivilege(Vps4User user, Privilege privilege) {
        if (!checkPrivilege(user, privilege)) {
            throw new AuthorizationException(user.getShopperId() + " does not have privilege " + privilege.name());
        }
    }

    @Override
    public boolean checkPrivilege(Vps4User user, Privilege privilege) {
        return Sql.with(dataSource).call(
                "{ call check_privilege(?,?,?) }",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), null, privilege.id());
    }

    @Override
    public void requireAnyPrivilegeToVmId(Vps4User user, UUID id) {
        if (!Sql.with(dataSource).call("SELECT COUNT(privilege_id) " +
                        "FROM user_project_privilege " +
                        "JOIN virtual_machine " +
                        "ON virtual_machine.project_id = user_project_privilege.project_id " +
                        "WHERE user_project_privilege.vps4_user_id = ? " +
                        "AND virtual_machine.vm_id = ?; ",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), id)) {
            throw new AuthorizationException(user.getShopperId() + " does not have privilege for vm " + id);
        }
    }


    @Override
    public void requireAnyPrivilegeToProjectId(Vps4User user, long projectId) {
        if (!checkAnyPrivilegeToProjectId(user, projectId)) {
            throw new AuthorizationException(
                    user.getShopperId() + " does not have privilege on service group " + projectId);
        }
    }

    @Override
    public boolean checkAnyPrivilegeToProjectId(Vps4User user, long projectId) {
        return Sql.with(dataSource).call(
                "{ call check_any_privilege(?,?) }",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), projectId);
    }

    @Override
    public boolean checkPrivilege(Vps4User user, long projectId, ProjectPrivilege privilege) {
        return Sql.with(dataSource).call(
                "{ call check_privilege(?,?,?) }",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), projectId, privilege.id());
    }

    @Override
    public void requirePrivilege(Vps4User user, long projectId, ProjectPrivilege privilege) {
        if (!checkPrivilege(user, projectId, privilege)) {
            throw new AuthorizationException(user.getShopperId() + " does not have privilege " + privilege
                    .name() + " on service group " + projectId);
        }
    }

    @Override
    public void outdateVmPrivilegeForShopper(long userId, long projectId) {
        Sql.with(dataSource)
                .exec("UPDATE user_project_privilege SET valid_until=now_utc() WHERE vps4_user_id=? AND project_id=? " +
                                "AND valid_until='infinity'",
                        null, userId, projectId);
    }

    @Override
    public void addPrivilegeForUser(long userId, int privilegeId, long projectId) {
        Sql.with(dataSource)
                .exec("INSERT INTO user_project_privilege (vps4_user_id, privilege_id, project_id, valid_on, " +
                                "valid_until) VALUES (?, ?, ?, now_utc(), 'infinity')",
                        null, userId, privilegeId, projectId);
    }

    @Override
    public UserProjectPrivilege getActivePrivilege(long projectId) {
        return Sql.with(dataSource)
                .exec("SELECT * FROM user_project_privilege upp WHERE upp.project_id=? AND upp.valid_until='infinity'",
                        Sql.nextOrNull(this::mapPrivilege), projectId);
    }


    protected UserProjectPrivilege mapPrivilege(ResultSet rs) throws SQLException {

        Timestamp validUntil = rs.getTimestamp("valid_until", TimestampUtils.utcCalendar);

        Timestamp validOn = rs.getTimestamp("valid_on", TimestampUtils.utcCalendar);
        return new UserProjectPrivilege(rs.getInt("privilege_id"), rs.getLong("project_id"), rs.getLong("vps4_user_id"),
                validOn != null ? validOn.toInstant() : null, validUntil != null ? validUntil.toInstant() : null);
    }

}
