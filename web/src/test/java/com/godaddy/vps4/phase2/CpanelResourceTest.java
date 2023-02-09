package com.godaddy.vps4.phase2;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.cpanel.CPanelAccountCacheStatus;
import com.godaddy.vps4.cpanel.CPanelSession;
import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelInvalidUserException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.cpanel.UpdateNginxRequest;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.controlPanel.cpanel.CPanelResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CpanelResourceTest {
    private Action conflictingAction = mock(Action.class);
    private VmResource vmResource = mock(VmResource.class);
    private Vps4CpanelService vps4CpanelService = mock(Vps4CpanelService.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private Config config = mock(Config.class);

    private GDUser user;
    private VirtualMachine vm;
    private VirtualMachine centVm;

    private String expectedVersion = "11.106.0.8";
    private String[] expectedPackages = {"foobar", "helloworld"};
    private CPanelAccountCacheStatus cacheStatus = new CPanelAccountCacheStatus("testuser", true);
    @Before
    public void setupTest() throws CpanelTimeoutException, CpanelAccessDeniedException {
        vm = createTestVm("hfs-centos-7-cpanel-11", Image.ControlPanel.CPANEL);
        centVm = createTestVm("hfs-centos-7", Image.ControlPanel.MYH);
        user = GDUserMock.createShopper();
        conflictingAction.type = ActionType.INSTALL_CPANEL_PACKAGE;
        Vm hfsVm = new Vm();
        hfsVm.status = "ACTIVE";
        Action testAction = new Action(123L, vm.vmId, ActionType.INSTALL_CPANEL_PACKAGE, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(),
                null);
        when(config.get("cpanel.rpm.packages", "")).thenReturn("ea-nginx,ea-nginx-http2,ea-nginx-brotli");
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(vmResource.getVm(centVm.vmId)).thenReturn(centVm);
        when(vmResource.getVmFromVmVertical(vm.hfsVmId)).thenReturn(hfsVm);
        when(vmResource.getVmFromVmVertical(centVm.hfsVmId)).thenReturn(hfsVm);
        when(actionService.getAction(anyLong())).thenReturn(testAction);
        when(actionService.createAction(vm.vmId, ActionType.INSTALL_CPANEL_PACKAGE, null, user.getUsername()))
                .thenReturn(testAction.id);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        when(vps4CpanelService.getVersion(vm.hfsVmId)).thenReturn(expectedVersion);
        when(vps4CpanelService.listInstalledRpmPackages(anyLong())).thenReturn(Arrays.asList(expectedPackages));
        when(vps4CpanelService.getNginxCacheConfig(anyLong())).thenReturn(Arrays.asList(cacheStatus));
    }

    private CPanelResource getcPanelResource() {
        return new CPanelResource(vmResource, vps4CpanelService, actionService, commandService, user, config);
    }

    private VirtualMachine createTestVm(String imageName, Image.ControlPanel controlPanel) {
        VirtualMachine vm = new VirtualMachine();
        vm.hfsVmId = 1234;
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.image = new Image();
        vm.image.imageName = imageName;
        vm.image.controlPanel = controlPanel;
        return vm;
    }


    // === whmSession Tests ===
    @Test
    public void testShopperGetWHMSession(){
        getcPanelResource().getWHMSession(vm.vmId);
    }

    @Test
    public void testAdminGetWHMSession(){
        user = GDUserMock.createAdmin();
        getcPanelResource().getWHMSession(vm.vmId);
    }

    @Test
    public void testGetWhmSessionInvalidImage(){
        try {
            getcPanelResource().getWHMSession(centVm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetWhmSessionIgnoresCpanelServiceException() throws Exception {
        when(vps4CpanelService.createSession(anyLong(), Mockito.anyString(), Mockito.any()))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getcPanelResource().getWHMSession(vm.vmId));
    }

    // === cpanelSession Tests ===
    @Test
    public void testShopperGetCPanelSession(){
        getcPanelResource().getCPanelSession(vm.vmId, "testuser", null, null);
    }

    @Test
    public void testAdminGetCPanelSession(){
        user = GDUserMock.createAdmin();
        getcPanelResource().getCPanelSession(vm.vmId, "testuser", null, null);
    }

    @Test
    public void testGetCPanelSessionInvalidImage(){
        try {
            getcPanelResource().getCPanelSession(centVm.vmId, "testuser", null, null);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetCPanelSessionWithInstallatronCommand() throws Exception {
        String url = "https://testUrl:2083/cpsess0000000/login/?session=blahblah%3acreate_user_session%2blahblah";
        CPanelSession session = new CPanelSession();
        CPanelSession.Data data = session.new Data();
        data.url = url;
        session.data = data;
        when(vps4CpanelService.createSession(anyLong(), Mockito.anyString(), Mockito.any()))
                .thenReturn(session);
        CPanelSession returnedSession = getcPanelResource().getCPanelSession(vm.vmId, "testuser", null, CPanelResource.InstallatronCommand.LIST_INSTALLED_APPS);
        Assert.assertEquals(url + "&goto_uri=3rdparty%2Finstallatron%2Findex.cgi%3F%23%2Finstalls%3F", returnedSession.data.url);
    }

    @Test
    public void testGetCPanelSessionWithInstallatronCommandAndAppId() throws Exception {
        String url = "https://testUrl:2083/cpsess0000000/login/?session=blahblah%3acreate_user_session%2blahblah";
        CPanelSession session = new CPanelSession();
        CPanelSession.Data data = session.new Data();
        data.url = url;
        session.data = data;
        when(vps4CpanelService.createSession(anyLong(), Mockito.anyString(), Mockito.any()))
                .thenReturn(session);
        CPanelSession returnedSession = getcPanelResource().getCPanelSession(vm.vmId, "testuser", "appId", CPanelResource.InstallatronCommand.MANAGE_APP);
        Assert.assertEquals(url + "&goto_uri=3rdparty%2Finstallatron%2Findex.cgi%3F%23%2Finstalls%2FappId", returnedSession.data.url);
    }

    @Test
    public void testGetCPanelSessionIgnoresCpanelServiceException() throws Exception {
        when(vps4CpanelService.createSession(anyLong(), Mockito.anyString(), Mockito.any()))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getcPanelResource().getCPanelSession(vm.vmId, "testuser", null, null));
    }

    // === listAccounts Tests ===
    @Test
    public void testShopperListCpanelAccounts(){
        getcPanelResource().listCpanelAccounts(vm.vmId);
    }

    @Test
    public void testAdminListCpanelAccounts(){
        user = GDUserMock.createAdmin();
        getcPanelResource().listCpanelAccounts(vm.vmId);
    }

    @Test
    public void testListCpanelAccountsInvalidImage(){
        try {
            getcPanelResource().listCpanelAccounts(centVm.vmId);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testListCpanelAccountsIgnoresCpanelServiceException() throws Exception {
        when(vps4CpanelService.listCpanelAccounts(anyLong()))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getcPanelResource().listCpanelAccounts(vm.vmId));
    }

    // list add on domains test
    @Test
    public void testShopperListAddonDomains(){
        getcPanelResource().listAddOnDomains(vm.vmId, "fakeuser");
    }

    @Test
    public void testAdminListAddonDomains(){
        user = GDUserMock.createAdmin();
        getcPanelResource().listAddOnDomains(vm.vmId, "fakeUser");
    }

    @Test
    public void testListAddonDomainsInvalidImage(){
        try {
            getcPanelResource().listAddOnDomains(centVm.vmId, "fakeuser");
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testListAddonDomainsIgnoresCpanelServiceException() throws Exception {
        when(vps4CpanelService.listAddOnDomains(anyLong(), eq("fakeuser")))
                .thenThrow(new CpanelTimeoutException("Timed out"));
        Assert.assertNull(getcPanelResource().listAddOnDomains(vm.vmId, "fakeuser"));
    }

    @Test(expected=Vps4Exception.class)
    public void testThrowsExceptionForInvalidUsername() throws Exception{
        when(vps4CpanelService.listAddOnDomains(anyLong(), eq("fakeuser2"))).thenThrow(new CpanelInvalidUserException(""));
        getcPanelResource().listAddOnDomains(vm.vmId, "fakeuser2");
    }

    // Calculate password strength
    @Test
    public void calculatePasswordStrengthCallsCpanelService() throws Exception {
        String password = "foobar";
        Long expectedStrength = 31L;
        when(vps4CpanelService.calculatePasswordStrength(anyLong(), eq(password))).thenReturn(expectedStrength);
        CPanelResource.PasswordStrengthRequest req = new CPanelResource.PasswordStrengthRequest();
        req.password = password;
        Long strength = getcPanelResource().calculatePasswordStrength(vm.vmId, req);
        Assert.assertEquals(expectedStrength, strength);
    }

    @Test
    public void calculatePasswordStrengthThrowsException() throws Exception {
        String password = "foobar";
        when(vps4CpanelService.calculatePasswordStrength(anyLong(), eq(password))).thenThrow(new RuntimeException());
        CPanelResource.PasswordStrengthRequest req = new CPanelResource.PasswordStrengthRequest();
        req.password = password;
        try {
            getcPanelResource().calculatePasswordStrength(vm.vmId, req);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("PASSWORD_STRENGTH_CALCULATION_FAILED", e.getId());
        }
    }

    // Create cpanel account
    @Test
    public void createAccountCallsCpanelService() {
        CPanelResource.CreateAccountRequest req = new CPanelResource.CreateAccountRequest();
        req.domainName = "domain";
        req.username = "user";
        req.plan = "plan";
        req.password = "foobar";
        req.contactEmail = "email@email.com";

        getcPanelResource().createAccount(vm.vmId, req);
    }

    @Test
    public void createPasswordThrowsException() throws Exception {
        String domainName = "domain";
        String username = "user";
        String password = "foobar";
        String plan = "plan";
        String email = "email@email.com";
        when(vps4CpanelService.createAccount(vm.hfsVmId, domainName, username, password, plan, email))
            .thenThrow(new RuntimeException());
        try {
            CPanelResource.CreateAccountRequest req = new CPanelResource.CreateAccountRequest();
            req.domainName = domainName;
            req.username = username;
            req.plan = plan;
            req.password = password;
            req.contactEmail = email;
            getcPanelResource().createAccount(vm.vmId, req);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("CREATE_CPANEL_ACCOUNT_FAILED", e.getId());
        }
    }

    // List packages
    @Test
    public void listPackagesCallsCpanelService() throws Exception {
        String[] expectedPackages = {"foobar", "helloworld"};
        when(vps4CpanelService.listPackages(anyLong())).thenReturn(Arrays.asList(expectedPackages));
        List<String> packages = getcPanelResource().listPackages(vm.vmId);
        Assert.assertArrayEquals(expectedPackages, packages.toArray());
    }

    @Test
    public void listPackagesThrowsException() throws Exception {
        when(vps4CpanelService.listPackages(vm.hfsVmId)).thenThrow(new RuntimeException());
        try {
            getcPanelResource().listPackages(vm.vmId);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("LIST_PACKAGES_FAILED", e.getId());
        }
    }

    // get version
    @Test
    public void getVersionCallsCpanelService() throws Exception {
        CPanelResource.CpanelVersionResponse response = getcPanelResource().getVersion(vm.vmId);
        Assert.assertEquals(expectedVersion, response.version);
    }

    @Test
    public void getVersionThrowsException() throws Exception {
        when(vps4CpanelService.getVersion(vm.hfsVmId)).thenThrow(new RuntimeException());
        try {
            getcPanelResource().getVersion(vm.vmId);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("GET_VERSION_FAILED", e.getId());
        }
    }

    @Test
    public void getVersionThrowsVersionFormatException() throws Exception {
        when(vps4CpanelService.getVersion(vm.hfsVmId)).thenReturn("11.000.0.8.23");
        try {
            getcPanelResource().getVersion(vm.vmId);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INCORRECT_VERSION_FORMAT", e.getId());
        }
    }

    // get nginx manager status
    @Test
    public void getNginxManagerStatusCallsCpanelService() {
        CPanelResource.CpanelNginxStatusResponse response = getcPanelResource().getNginxManagerStatus(vm.vmId);
        Assert.assertEquals(CPanelResource.NginxStatus.INSTALLABLE, response.installStatus);
        Assert.assertEquals(0, response.accountCachingStatus.size());
    }

    @Test
    public void getNginxManagerStatusReturnsNotInstallable() throws CpanelTimeoutException, CpanelAccessDeniedException {
        when(vps4CpanelService.getVersion(vm.hfsVmId)).thenReturn("11.000.0.8");
        CPanelResource.CpanelNginxStatusResponse response = getcPanelResource().getNginxManagerStatus(vm.vmId);
        Assert.assertEquals(CPanelResource.NginxStatus.NOT_INSTALLABLE, response.installStatus);
        Assert.assertEquals(0, response.accountCachingStatus.size());
    }

    @Test
    public void getNginxManagerStatusReturnsInstalled() throws Exception {
        when(vps4CpanelService.listInstalledRpmPackages(anyLong())).thenReturn(Collections.singletonList("ea-nginx"));
        CPanelResource.CpanelNginxStatusResponse response = getcPanelResource().getNginxManagerStatus(vm.vmId);
        Assert.assertEquals(CPanelResource.NginxStatus.INSTALLED, response.installStatus);
        Assert.assertEquals(1, response.accountCachingStatus.size());
    }

    @Test
    public void getNginxManagerStatusThrowsException() throws Exception {
        when(vps4CpanelService.listInstalledRpmPackages(vm.hfsVmId)).thenThrow(new RuntimeException());
        try {
            getcPanelResource().getNginxManagerStatus(vm.vmId);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("GET_NGINX_STATUS_FAILED", e.getId());
        }
    }
    // Install packages
    @Test
    public void installPackagesCallsCommandService() {
        CPanelResource.InstallPackageRequest req = new CPanelResource.InstallPackageRequest();
        req.packageName = "ea-nginx";
        getcPanelResource().installRpmPackage(vm.vmId, req);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4InstallCPanelPackage", argument.getValue().commands.get(0).command);
    }

    @Test
    public void installPackagesCreatesAction() {
        CPanelResource.InstallPackageRequest req = new CPanelResource.InstallPackageRequest();
        req.packageName = "ea-nginx";
        getcPanelResource().installRpmPackage(vm.vmId, req);
        verify(actionService, times(1)).createAction(vm.vmId, ActionType.INSTALL_CPANEL_PACKAGE,
                "{\"packageName\":\"ea-nginx\"}", user.getUsername());

    }

    @Test
    public void installPackagesPackageNotAllowed() {
        CPanelResource.InstallPackageRequest req = new CPanelResource.InstallPackageRequest();
        req.packageName = "ea-nginx-random";
        try {
            getcPanelResource().installRpmPackage(vm.vmId, req);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("PACKAGE_NOT_ALLOWED", e.getId());
        }
    }

    @Test
    public void installPackagesConflictingAction() {
        when(actionService.getIncompleteActions(vm.vmId)).thenReturn(Collections.singletonList(conflictingAction));
        CPanelResource.InstallPackageRequest req = new CPanelResource.InstallPackageRequest();
        req.packageName = "ea-nginx";
        try {
            getcPanelResource().installRpmPackage(vm.vmId, req);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    // Enable or disable NGiNX
    @Test
    public void updateNginxCallsCpanelService() {
        UpdateNginxRequest req = new UpdateNginxRequest();
        req.usernames = Arrays.asList("vpsdev1", "vpsdev2");
        req.enabled = true;

        getcPanelResource().updateNginx(vm.vmId, req);
    }

    @Test(expected = Vps4Exception.class)
    public void updateNginxThrowsException() throws Exception {
        UpdateNginxRequest req = new UpdateNginxRequest();
        req.usernames = Arrays.asList("vpsdev1", "vpsdev2");
        req.enabled = true;

        when(vps4CpanelService.updateNginx(vm.hfsVmId, true, req.usernames)).thenThrow(new RuntimeException());

        getcPanelResource().updateNginx(vm.vmId, req);
    }

    // Clear NGiNX cache
    @Test
    public void clearNginxCacheCallsCpanelService() throws CpanelTimeoutException, IOException, CpanelAccessDeniedException {
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");

        getcPanelResource().clearNginxCache(vm.vmId, usernames);

        verify(vps4CpanelService,times(1)).clearNginxCache(vm.hfsVmId, usernames);
    }

    @Test(expected = Vps4Exception.class)
    public void clearNginxCacheThrowsException() throws Exception {
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        when(vps4CpanelService.clearNginxCache(vm.hfsVmId, usernames)).thenThrow(new RuntimeException());

        getcPanelResource().clearNginxCache(vm.vmId, usernames);
    }
}
