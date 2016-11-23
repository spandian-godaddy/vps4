package com.godaddy.vps4.phase2.vmuser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.jdbc.JdbcVmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcVmUserServiceTest {

    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource = injector.getInstance(DataSource.class);
    ProjectService projectService = new JdbcProjectService(dataSource);

    private long projectId;
    private long vmId;
    private UUID orionGuid = UUID.randomUUID();
    private String username = "testuser";

    @Before
    public void setupServers() throws SQLException {
        projectId = projectService.createProject("testNetwork", 1, 1).getProjectId();
        vmId = SqlTestData.insertTestVm(orionGuid, projectId, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        SqlTestData.cleanupTestProject(projectId, dataSource);
    }

    @Test
    public void testCreateUser() throws SQLException {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vmId, true);
        List<VmUser> ul = service.listUsers(vmId);
        assertEquals(1, ul.size());
        VmUser usr = ul.get(0);
        assertEquals(username, usr.username);
        assertEquals(vmId, usr.vmId);
        assertTrue(usr.adminEnabled);
    }

    @Test
    public void testCreateUserNoAdmin() throws SQLException {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vmId, false);
        List<VmUser> ul = service.listUsers(vmId);
        assertEquals(1, ul.size());
        assertEquals(1, ul.size());
        VmUser usr = ul.get(0);
        assertEquals(username, usr.username);
        assertEquals(vmId, usr.vmId);
        assertFalse(usr.adminEnabled);
    }

    @Test
    public void testCreate2ndUser() {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vmId, false);
        service.createUser("testuser2", vmId, false);
        List<VmUser> ul = service.listUsers(vmId);
        assertEquals(2, ul.size());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateUserAlreadyExists() throws SQLException {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vmId, false);
        service.createUser(username, vmId, false);
        fail("Should throw exception");
    }

    @Test
    public void testUpdateUserAdminAccess() {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vmId, false);
        List<VmUser> ul = service.listUsers(vmId);
        VmUser u = ul.get(0);
        assertFalse(u.adminEnabled);
        service.updateUserAdminAccess(username, vmId, true);
        ul = service.listUsers(vmId);
        u = ul.get(0);
        assertTrue(u.adminEnabled);
    }

}
