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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcVmUserServiceTest {

    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource = injector.getInstance(DataSource.class);
    ProjectService projectService = new JdbcProjectService(dataSource);
    VirtualMachineService vmService = new JdbcVirtualMachineService(dataSource);

    private UUID orionGuid = UUID.randomUUID();
    private String username = "testuser";
    private VirtualMachine vm;

    @Before
    public void setupServers() throws SQLException {
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void testCreateUser() throws SQLException {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vm.vmId, true, VmUserType.CUSTOMER);
        List<VmUser> ul = service.listUsers(vm.vmId, null);
        assertEquals(1, ul.size());
        VmUser usr = ul.get(0);
        assertEquals(username, usr.username);
        assertEquals(vm.vmId, usr.vmId);
        assertTrue(usr.adminEnabled);
        assertEquals(VmUserType.CUSTOMER, usr.vmUserType);
    }

    @Test
    public void testCreateUserNoAdmin() throws SQLException {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vm.vmId, false);
        List<VmUser> ul = service.listUsers(vm.vmId, null);
        assertEquals(1, ul.size());
        assertEquals(1, ul.size());
        VmUser usr = ul.get(0);
        assertEquals(username, usr.username);
        assertEquals(vm.vmId, usr.vmId);
        assertFalse(usr.adminEnabled);
    }

    @Test
    public void testCreate2ndUser() {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vm.vmId, false);
        service.createUser("testuser2", vm.vmId, false);
        List<VmUser> ul = service.listUsers(vm.vmId, null);
        assertEquals(2, ul.size());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateUserAlreadyExists() throws SQLException {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vm.vmId, false);
        service.createUser(username, vm.vmId, false);
        fail("Should throw exception");
    }

    @Test
    public void testUpdateUserAdminAccess() {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vm.vmId, false);
        List<VmUser> ul = service.listUsers(vm.vmId, null);
        VmUser u = ul.get(0);
        assertFalse(u.adminEnabled);
        service.updateUserAdminAccess(username, vm.vmId, true);
        ul = service.listUsers(vm.vmId, null);
        u = ul.get(0);
        assertTrue(u.adminEnabled);
    }

    @Test
    public void testDeleteUser() {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username, vm.vmId, true);
        List<VmUser> ul = service.listUsers(vm.vmId, null);
        assertEquals(1, ul.size());
        service.deleteUser(username, vm.vmId);
        ul = service.listUsers(vm.vmId, null);
        assertEquals(0, ul.size());
    }

    @Test
    public void getUsersWithType() {
        VmUserService service = new JdbcVmUserService(dataSource);
        service.createUser(username + "1", vm.vmId, true, VmUserType.CUSTOMER);
        service.createUser(username + "2", vm.vmId, true, VmUserType.SUPPORT);
        List<VmUser> ul = service.listUsers(vm.vmId, null);
        assertEquals(2, ul.size());
        ul = service.listUsers(vm.vmId, VmUserType.CUSTOMER);
        assertEquals(1, ul.size());
        assertEquals(username + "1", ul.get(0).username);
        ul = service.listUsers(vm.vmId, VmUserType.SUPPORT);
        assertEquals(1, ul.size());
        assertEquals(username + "2", ul.get(0).username);
    }
}
