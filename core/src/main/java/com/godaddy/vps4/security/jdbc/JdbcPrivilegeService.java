package com.godaddy.vps4.security.jdbc;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.ProjectPrivilege;
import com.godaddy.vps4.security.Privilege;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.User;



public class JdbcPrivilegeService implements PrivilegeService {

    private final DataSource dataSource;

    @Inject
    public JdbcPrivilegeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void requirePrivilege(User user, Privilege privilege) {
        if (!checkPrivilege(user, privilege)) {
            throw new AuthorizationException(user.getShopperId() + " does not have privilege " + privilege.name());
        }
    }

    @Override
    public boolean checkPrivilege(User user, Privilege privilege) {
        return Sql.with(dataSource).call(
                "{ call check_privilege(?,?,?) }",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), null, privilege.id());
    }

    @Override
    public void requireAnyPrivilegeToSgid(User user, long sgid) {
        if (!checkAnyPrivilegeToSgid(user, sgid)) {
            throw new AuthorizationException(user.getShopperId() + " does not have privilege on service group " + sgid);
        }
    }

    @Override
    public boolean checkAnyPrivilegeToSgid(User user, long sgid) {
        return Sql.with(dataSource).call(
                "{ call check_any_privilege(?,?) }",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), sgid);
    }

    @Override
    public boolean checkPrivilege(User user, long sgid, ProjectPrivilege privilege) {
        return Sql.with(dataSource).call(
                "{ call check_privilege(?,?,?) }",
                Sql.nextOrNull(rs -> rs.getInt(1) > 0),
                user.getId(), sgid, privilege.id());
    }

    @Override
    public void requirePrivilege(User user, long sgid, ProjectPrivilege privilege) {
        if (!checkPrivilege(user, sgid, privilege)) {
            throw new AuthorizationException(user.getShopperId() + " does not have privilege " + privilege.name() + " on service group " + sgid);
        }
    }

}
