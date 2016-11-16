package com.godaddy.vps4.phase2.user;

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
import com.godaddy.vps4.vm.User;
import com.godaddy.vps4.vm.UserService;
import com.godaddy.vps4.vm.jdbc.JdbcUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcUserServiceTest {

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
        SqlTestData.cleanupTestVmAndRelatedData(vmId, orionGuid, dataSource);
        SqlTestData.cleanupTestProject(projectId, dataSource);
    }

    @Test
    public void testCreateUser() throws SQLException {
        UserService service = new JdbcUserService(dataSource);
        service.createUser(username, vmId, true);
        List<User> ul = service.listUsers(vmId);
        assertEquals(1, ul.size());
        User usr = ul.get(0);
        assertEquals(username, usr.username);
        assertEquals(vmId, usr.vmId);
        assertTrue(usr.adminEnabled);
    }

    @Test
    public void testCreateUserNoAdmin() throws SQLException {
        UserService service = new JdbcUserService(dataSource);
        service.createUser(username, vmId, false);
        List<User> ul = service.listUsers(vmId);
        assertEquals(1, ul.size());
        assertEquals(1, ul.size());
        User usr = ul.get(0);
        assertEquals(username, usr.username);
        assertEquals(vmId, usr.vmId);
        assertFalse(usr.adminEnabled);
    }

    @Test
    public void testCreate2ndUser() {
        UserService service = new JdbcUserService(dataSource);
        service.createUser(username, vmId, false);
        service.createUser("testuser2", vmId, false);
        List<User> ul = service.listUsers(vmId);
        assertEquals(2, ul.size());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateUserAlreadyExists() throws SQLException {
        UserService service = new JdbcUserService(dataSource);
        service.createUser(username, vmId, false);
        service.createUser(username, vmId, false);
        fail("Should throw exception");
    }

    @Test
    public void testUpdateUserAdminAccess() {
        UserService service = new JdbcUserService(dataSource);
        service.createUser(username, vmId, false);
        List<User> ul = service.listUsers(vmId);
        User u = ul.get(0);
        assertFalse(u.adminEnabled);
        service.updateUserAdminAccess(username, vmId, true);
        ul = service.listUsers(vmId);
        u = ul.get(0);
        assertTrue(u.adminEnabled);
    }

}
