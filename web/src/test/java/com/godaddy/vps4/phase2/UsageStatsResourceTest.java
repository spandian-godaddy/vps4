package com.godaddy.vps4.phase2;

import java.text.ParseException;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.sysadmin.VmUsageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.sysadmin.UsageStatsResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import junit.framework.Assert;

public class UsageStatsResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    private VmUsageService vmUsageService;

    private GDUser user;
    private VirtualMachine vm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    vmUsageService = Mockito.mock(VmUsageService.class);
                    bind(VmUsageService.class).toInstance(vmUsageService);
                }

                @Provides
                protected GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        vm = createTestVm();
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private UsageStatsResource getUsageStatsResource() {
        return injector.getInstance(UsageStatsResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    // === getUsage Tests ===
    @Test
    public void testShopperGetUsage(){
        getUsageStatsResource().getUsage(vm.vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetUsage(){
        user = GDUserMock.createShopper("shopperX");
        getUsageStatsResource().getUsage(vm.vmId);
    }

    @Test
    public void testAdminGetUsage(){
        user = GDUserMock.createAdmin();
        getUsageStatsResource().getUsage(vm.vmId);
    }

    @Test
    public void testGetUsageParseException() throws ParseException {
        Mockito.when(vmUsageService.getUsage(Mockito.anyLong()))
                .thenThrow(new ParseException("parse error", 0));
        try {
            getUsageStatsResource().getUsage(vm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("BAD_USAGE_DATA", e.getId());
        }
    }

}
