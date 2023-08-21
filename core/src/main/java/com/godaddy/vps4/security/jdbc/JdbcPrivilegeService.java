package com.godaddy.vps4.security.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import java.util.UUID;


public class JdbcPrivilegeService implements PrivilegeService {

    private final DataSource dataSource;

    @Inject
    public JdbcPrivilegeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void requireAnyPrivilegeToVmId(Vps4User user, UUID id) {
        if (!Sql.with(dataSource).call("SELECT vps4_user_id " +
                        "FROM project " +
                        "JOIN virtual_machine " +
                        "ON virtual_machine.project_id = project.project_id " +
                        "AND virtual_machine.vm_id = ?; ",
                Sql.nextOrNull(rs -> rs.getLong(1) == user.getId()), id)) {
            throw new ForbiddenException(user.getShopperId() + " does not have privilege for vm " + id);
        }
    }


    @Override
    public void requireAnyPrivilegeToProjectId(Vps4User user, long projectId) {
        if (!checkAnyPrivilegeToProjectId(user, projectId)) {
            throw new ForbiddenException(
                    user.getShopperId() + " does not have privilege on service group " + projectId);
        }
    }

    @Override
    public boolean checkAnyPrivilegeToProjectId(Vps4User user, long projectId) {
        return Sql.with(dataSource).exec("SELECT vps4_user_id FROM project WHERE project_id = ?",
                Sql.nextOrNull(rs -> rs.getLong(1) == user.getId()), projectId);
    }

}
