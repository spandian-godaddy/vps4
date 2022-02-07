package com.godaddy.vps4.phase2;

import com.godaddy.vps4.cpanel.CpanelInvalidUserException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

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
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    cpServ = mock(Vps4CpanelService.class);
                    bind(Vps4CpanelService.class).toInstance(cpServ);
                    SchedulerWebService swServ = mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
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
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1", UUID.randomUUID());
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
        Mockito.when(cpServ.createSession(anyLong(), Mockito.anyString(), Mockito.any()))
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
        Mockito.when(cpServ.createSession(anyLong(), Mockito.anyString(), Mockito.any()))
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
        Mockito.when(cpServ.listCpanelAccounts(anyLong()))
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

    // list add on domains test
    @Test
    public void testShopperListAddonDomains(){
        getCpanelResource().listAddOnDomains(vm.vmId, "fakeuser");
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperListAddonDomains(){
        user = GDUserMock.createShopper("shopperX");
        getCpanelResource().listAddOnDomains(vm.vmId, "fakeuser");
    }

    @Test
    public void testAdminListAddonDomains(){
        user = GDUserMock.createAdmin();
        getCpanelResource().listAddOnDomains(vm.vmId, "fakeUser");
    }

    @Test
    public void testListAddonDomainsInvalidImage(){
        try {
            getCpanelResource().listAddOnDomains(centVm.vmId, "fakeuser");
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test(expected=NotFoundException.class)
    public void testListAddonDomainsVmIdDoesntExist(){
        getCpanelResource().listAddOnDomains(UUID.randomUUID(), "fakeuser");
    }

    @Test
    public void testListAddonDomainsIgnoresCpanelServiceException() throws Exception {
        Mockito.when(cpServ.listAddOnDomains(anyLong(), eq("fakeuser")))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getCpanelResource().listAddOnDomains(vm.vmId, "fakeuser"));
    }

    @Test(expected=Vps4Exception.class)
    public void testThrowsExceptionForInvalidUsername() throws Exception{
        Mockito.when(cpServ.listAddOnDomains(anyLong(), eq("fakeuser2"))).thenThrow(new CpanelInvalidUserException(""));
        getCpanelResource().listAddOnDomains(vm.vmId, "fakeuser2");
    }


    @Test
    public void testListAddonDomainsSuspended() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            getCpanelResource().listAddOnDomains(vm.vmId, "fakeuser");
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    // Calculate password strength
    @Test
    public void calculatePasswordStrengthCallsCpanelService() throws Exception {
        String password = "foobar";
        Long expectedStrength = 31L;
        Mockito.when(cpServ.calculatePasswordStrength(anyLong(), eq(password))).thenReturn(expectedStrength);
        CPanelResource.PasswordStrengthRequest req = new CPanelResource.PasswordStrengthRequest();
        req.password = password;
        Long strength = getCpanelResource().calculatePasswordStrength(vm.vmId, req);
        Assert.assertEquals(expectedStrength, strength);
    }

    @Test
    public void calculatePasswordStrengthThrowsException() throws Exception {
        String password = "foobar";
        Mockito.when(cpServ.calculatePasswordStrength(anyLong(), eq(password))).thenThrow(new RuntimeException());
        CPanelResource.PasswordStrengthRequest req = new CPanelResource.PasswordStrengthRequest();
        req.password = password;
        try {
            getCpanelResource().calculatePasswordStrength(vm.vmId, req);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("PASSWORD_STRENGTH_CALCULATION_FAILED", e.getId());
        }
    }

    // Create cpanel account
    @Test
    public void createAccountCallsCpanelService() throws Exception {
        String domainName = "domain";
        String username = "user";
        String password = "foobar";
        String plan = "plan";
        String email = "email@email.com";

        CPanelResource.CreateAccountRequest req = new CPanelResource.CreateAccountRequest();
        req.domainName = domainName;
        req.username = username;
        req.plan = plan;
        req.password = password;
        req.contactEmail = email;
        try {
            getCpanelResource().createAccount(vm.vmId, req);
        }
        catch (Exception e) {
            Assert.fail("This test shouldn't fail");
        }
    }

    @Test
    public void createPasswordThrowsException() throws Exception {
        String domainName = "domain";
        String username = "user";
        String password = "foobar";
        String plan = "plan";
        String email = "email@email.com";
        Mockito.when(cpServ.createAccount(vm.hfsVmId, domainName, username, password, plan, email))
            .thenThrow(new RuntimeException());
        try {
            CPanelResource.CreateAccountRequest req = new CPanelResource.CreateAccountRequest();
            req.domainName = domainName;
            req.username = username;
            req.plan = plan;
            req.password = password;
            req.contactEmail = email;
            getCpanelResource().createAccount(vm.vmId, req);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("CREATE_CPANEL_ACCOUNT_FAILED", e.getId());
        }
    }

    // List packages
    @Test
    public void listPackagesCallsCpanelService() throws Exception {
        String[] expectedPackages = {"foobar", "helloworld"};
        Mockito.when(cpServ.listPackages(anyLong())).thenReturn(Arrays.asList(expectedPackages));
        List<String> packages = getCpanelResource().listPackages(vm.vmId);
        Assert.assertArrayEquals(expectedPackages, packages.toArray());
    }

    @Test
    public void listPackagesThrowsException() throws Exception {
        Mockito.when(cpServ.listPackages(vm.hfsVmId)).thenThrow(new RuntimeException());
        try {
            getCpanelResource().listPackages(vm.vmId);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("LIST_PACKAGES_FAILED", e.getId());
        }
    }

}
