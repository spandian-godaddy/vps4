package com.godaddy.vps4.phase2;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserType;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.UserResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.mockito.Mockito;

public class UserResourceTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;
    private VirtualMachine testVm;

    private VmUser testUser;
    private VmUser testSupportUser1;
    private VmUser testSupportUser2;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private UserResource getUserResource() {
        return injector.getInstance(UserResource.class);
    }

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        testVm = createTestVm();
        testUser = new VmUser("testUser", testVm.vmId, true, VmUserType.CUSTOMER);
        testSupportUser1 = new VmUser("testSupportUser1", testVm.vmId, true, VmUserType.SUPPORT);
        testSupportUser2 = new VmUser("testSupportUser2", testVm.vmId, true, VmUserType.SUPPORT);
        SqlTestData.insertTestUser(testUser, dataSource);
        SqlTestData.insertTestUser(testSupportUser1, dataSource);
        SqlTestData.insertTestUser(testSupportUser2, dataSource);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    @Test
    public void testGetUsers() {
        List<VmUser> users = getUserResource().getUsers(testVm.vmId, null);
        Assert.assertEquals(3, users.size());
        Assert.assertTrue(users.stream().anyMatch(o -> o.username.equals(testUser.username)));
        Assert.assertTrue(users.stream().anyMatch(o -> o.username.equals(testSupportUser1.username)));
        Assert.assertTrue(users.stream().anyMatch(o -> o.username.equals(testSupportUser2.username)));
    }

    @Test
    public void testGetCustomerUsers() {
        List<VmUser> users = getUserResource().getUsers(testVm.vmId, VmUserType.CUSTOMER);
        Assert.assertEquals(1, users.size());
        Assert.assertTrue(users.stream().anyMatch(o -> o.username.equals(testUser.username)));
    }

    @Test
    public void testGetSupportUsers() {
        List<VmUser> users = getUserResource().getUsers(testVm.vmId, VmUserType.SUPPORT);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.stream().anyMatch(o -> o.username.equals(testSupportUser1.username)));
        Assert.assertTrue(users.stream().anyMatch(o -> o.username.equals(testSupportUser2.username)));
    }
}
