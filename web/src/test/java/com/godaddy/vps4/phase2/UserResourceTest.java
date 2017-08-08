package com.godaddy.vps4.phase2;

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
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class UserResourceTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;
    private VirtualMachine testVm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new Phase2ExternalsModule() {

                @Override
                public void configure() {
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
        VmUser testUser = new VmUser("testUser", testVm.vmId, true, VmUserType.CUSTOMER);
        VmUser testSupportUser1 = new VmUser("testSupportUser1", testVm.vmId, true, VmUserType.SUPPORT);
        VmUser testSupportUser2 = new VmUser("testSupportUser2", testVm.vmId, true, VmUserType.SUPPORT);
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
        Assert.assertEquals("testUser", users.get(0).username);
        Assert.assertEquals("testSupportUser1", users.get(1).username);
        Assert.assertEquals("testSupportUser2", users.get(2).username);
    }

    @Test
    public void testGetCustomerUsers() {
        List<VmUser> users = getUserResource().getUsers(testVm.vmId, VmUserType.CUSTOMER);
        Assert.assertEquals(1, users.size());
        Assert.assertEquals("testUser", users.get(0).username);
    }

    @Test
    public void testGetSupportUsers() {
        List<VmUser> users = getUserResource().getUsers(testVm.vmId, VmUserType.SUPPORT);
        Assert.assertEquals(2, users.size());
        Assert.assertEquals("testSupportUser1", users.get(0).username);
        Assert.assertEquals("testSupportUser2", users.get(1).username);
    }
}
