package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.controlPanel.cpanel.CPanelResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class CpanelResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    private Vps4CpanelService cpServ;

    private GDUser user;
    private VirtualMachine vm;
    private VirtualMachine centVm;

    Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    cpServ = Mockito.mock(Vps4CpanelService.class);
                    bind(Vps4CpanelService.class).toInstance(cpServ);
                }

                @Provides
                GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        vm = createTestVm("hfs-centos-7-cpanel-11");
        centVm = createTestVm("hfs-centos-7");
    }

    private VirtualMachine createTestVm(String imageName) {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource, imageName);
        return vm;
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private CPanelResource getCpanelResource() {
        return injector.getInstance(CPanelResource.class);
    }

    // === whmSession Tests ===
    @Test
    public void testShopperGetWHMSession(){
        getCpanelResource().getWHMSession(vm.vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetWHMSession(){
        user = GDUserMock.createShopper("shopperX");
        getCpanelResource().getWHMSession(vm.vmId);
    }

    @Test
    public void testAdminGetWHMSession(){
        user = GDUserMock.createAdmin();
        getCpanelResource().getWHMSession(vm.vmId);
    }

    @Test
    public void testGetWhmSessionInvalidImage(){
        try {
            getCpanelResource().getWHMSession(centVm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetWhmSessionIgnoresCpanelServiceException() throws Exception {
        Mockito.when(cpServ.createSession(Mockito.anyLong(), Mockito.anyString(), Mockito.any()))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getCpanelResource().getWHMSession(vm.vmId));
    }

    // === cpanelSession Tests ===
    @Test
    public void testShopperGetCPanelSession(){
        getCpanelResource().getCPanelSession(vm.vmId, "testuser");
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetCPanelSession(){
        user = GDUserMock.createShopper("shopperX");
        getCpanelResource().getCPanelSession(vm.vmId, "testuser");
    }

    @Test
    public void testAdminGetCPanelSession(){
        user = GDUserMock.createAdmin();
        getCpanelResource().getCPanelSession(vm.vmId, "testuser");
    }

    @Test
    public void testGetCPanelSessionInvalidImage(){
        try {
            getCpanelResource().getCPanelSession(centVm.vmId, "testuser");
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetCPanelSessionIgnoresCpanelServiceException() throws Exception {
        Mockito.when(cpServ.createSession(Mockito.anyLong(), Mockito.anyString(), Mockito.any()))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getCpanelResource().getCPanelSession(vm.vmId, "testuser"));
    }

    // === listAccounts Tests ===
    @Test
    public void testShopperListCpanelAccounts(){
        getCpanelResource().listCpanelAccounts(vm.vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperListCpanelAccounts(){
        user = GDUserMock.createShopper("shopperX");
        getCpanelResource().listCpanelAccounts(vm.vmId);
    }

    @Test
    public void testAdminListCpanelAccounts(){
        user = GDUserMock.createAdmin();
        getCpanelResource().listCpanelAccounts(vm.vmId);
    }

    @Test
    public void testListCpanelAccountsInvalidImage(){
        try {
            getCpanelResource().listCpanelAccounts(centVm.vmId);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test(expected=NotFoundException.class)
    public void testVmIdDoesntExist(){
        getCpanelResource().listCpanelAccounts(UUID.randomUUID());
    }

    @Test
    public void testListCpanelAccountsIgnoresCpanelServiceException() throws Exception {
        Mockito.when(cpServ.listCpanelAccounts(Mockito.anyLong()))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getCpanelResource().listCpanelAccounts(vm.vmId));
    }

    @Test
    public void testListCpanelAccountsSuspended() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            getCpanelResource().listCpanelAccounts(centVm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

}
