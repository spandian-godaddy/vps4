package com.godaddy.vps4.orchestration.vm.rebuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.orchestration.cdn.Vps4RemoveCdnSite;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.messaging.SendSetupCompletedEmail;
import com.godaddy.vps4.orchestration.messaging.SetupCompletedEmailRequest;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4RebuildVmTest {

    ActionService actionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService vps4NetworkService = mock(NetworkService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    CreditService creditService = mock(CreditService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);
    NetworkService networkService = mock(NetworkService.class);
    ShopperNotesService shopperNotesService = mock(ShopperNotesService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);

    @Captor private ArgumentCaptor<SetupCompletedEmailRequest> setupEmailCaptor;

    UUID vps4VmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    String shopperId = "12345678";
    long originalHfsVmId = 123L;
    long newHfsVmId = 456L;
    long hfsAddressId = 34L;
    String fqdn = "10.0.0.1";
    VirtualMachine vm;
    VmAction action;

    UnbindIp unbindIp = mock(UnbindIp.class);
    DestroyVm destroyVm = mock(DestroyVm.class);
    CreateVm createVm = mock(CreateVm.class);
    WaitForAndRecordVmAction waitForVm = mock(WaitForAndRecordVmAction.class);
    BindIp bindIp = mock(BindIp.class);
    ConfigureCpanel configCpanel = mock(ConfigureCpanel.class);
    ConfigurePlesk configPlesk = mock(ConfigurePlesk.class);
    ToggleAdmin enableAdmin = mock(ToggleAdmin.class);
    SetPassword setPassword = mock(SetPassword.class);
    ConfigureMailRelay setMailRelay = mock(ConfigureMailRelay.class);
    SetupPanopta setupPanopta = mock(SetupPanopta.class);
    SendSetupCompletedEmail sendSetupCompletedEmail = mock(SendSetupCompletedEmail.class);
    Vps4RemoveCdnSite vps4RemoveCdnSite = mock(Vps4RemoveCdnSite.class);

    VmCdnSite vmCdnSite = mock(VmCdnSite.class);
    Vps4RebuildVm command;
    Vps4RebuildVm.Request request;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UnbindIp.class).toInstance(unbindIp);
        binder.bind(DestroyVm.class).toInstance(destroyVm);
        binder.bind(CreateVm.class).toInstance(createVm);
        binder.bind(WaitForAndRecordVmAction.class).toInstance(waitForVm);
        binder.bind(BindIp.class).toInstance(bindIp);
        binder.bind(ConfigureCpanel.class).toInstance(configCpanel);
        binder.bind(ConfigurePlesk.class).toInstance(configPlesk);
        binder.bind(ToggleAdmin.class).toInstance(enableAdmin);
        binder.bind(SetPassword.class).toInstance(setPassword);
        binder.bind(ConfigureMailRelay.class).toInstance(setMailRelay);
        binder.bind(SetupPanopta.class).toInstance(setupPanopta);
        binder.bind(SendSetupCompletedEmail.class).toInstance(sendSetupCompletedEmail);
        binder.bind(Vps4RemoveCdnSite.class).toInstance(vps4RemoveCdnSite);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);

        IpAddress publicIp = new IpAddress();
        publicIp.hfsAddressId = hfsAddressId;
        publicIp.ipAddress = fqdn;
        when(vps4NetworkService.getVmIpAddresses(vps4VmId)).thenReturn(Arrays.asList(publicIp));
        when(vps4NetworkService.getVmPrimaryAddress(vps4VmId)).thenReturn(publicIp);

        vm = new VirtualMachine();
        vm.orionGuid = orionGuid;
        vm.hfsVmId = originalHfsVmId;
        vm.image = new Image();
        vm.image.controlPanel = ControlPanel.CPANEL;
        vm.name = "server-name";
        vm.primaryIpAddress = publicIp;
        when(virtualMachineService.getVirtualMachine(vps4VmId)).thenReturn(vm);

        request = new Vps4RebuildVm.Request();
        request.rebuildVmInfo = new RebuildVmInfo();
        request.rebuildVmInfo.vmId = vps4VmId;
        request.rebuildVmInfo.orionGuid = vm.orionGuid;
        request.rebuildVmInfo.image = new Image();
        request.rebuildVmInfo.image.hfsName = "hfs-centos-7-cpanel-11";
        request.rebuildVmInfo.image.imageId = 7L;
        request.rebuildVmInfo.image.controlPanel = ControlPanel.CPANEL;
        request.rebuildVmInfo.rawFlavor = "raw-flavor";
        request.rebuildVmInfo.username = "user";
        request.rebuildVmInfo.serverName = vm.name;
        request.rebuildVmInfo.hostname = "host.name";
        request.rebuildVmInfo.encryptedPassword = "encrypted".getBytes();
        request.rebuildVmInfo.customerId = customerId;
        request.rebuildVmInfo.shopperId = shopperId;
        request.rebuildVmInfo.keepAdditionalIps = true;
        request.rebuildVmInfo.gdUserName = "fake-employee";
        request.rebuildVmInfo.ipAddress = publicIp;

        String messagedId = UUID.randomUUID().toString();
        when(sendSetupCompletedEmail.execute(any(CommandContext.class), any(SetupCompletedEmailRequest.class)))
                .thenReturn(messagedId);

        VmUser customerUser = new VmUser("customer", vps4VmId, false, VmUserType.CUSTOMER);
        VmUser supportUser = new VmUser("support-123", vps4VmId, true, VmUserType.SUPPORT);
        when(vmUserService.listUsers(vps4VmId)).thenReturn(Arrays.asList(customerUser, supportUser));

        vmCdnSite.siteId = "fakeSiteId";
        vmCdnSite.vmId = vps4VmId;
        when(cdnDataService.getActiveCdnSitesOfVm(vps4VmId)).thenReturn(Collections.singletonList(vmCdnSite));

        action = new VmAction();
        action.vmId = newHfsVmId;
        doReturn(action).when(context).execute(eq("CreateVm"), eq(CreateVm.class), any());

        command = new Vps4RebuildVm(actionService, virtualMachineService, vps4NetworkService, vmUserService,
                                    creditService, panoptaDataService, hfsVmTrackingRecordService, networkService,
                                    shopperNotesService, cdnDataService);
    }

    @Test
    public void setsShopperNote() {
        command.execute(context, request);
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(shopperNotesService, times(1))
                .processShopperMessage(eq(vps4VmId), argument.capture());
        String value = argument.getValue();
        Assert.assertTrue(value.contains("rebuilt"));
        Assert.assertTrue(value.contains(vps4VmId.toString()));
        Assert.assertTrue(value.contains(vm.orionGuid.toString()));
        Assert.assertTrue(value.contains(request.rebuildVmInfo.gdUserName));
    }

    @Test
    public void getsPublicIpAddresses() {
        command.execute(context, request);
        verify(vps4NetworkService, atLeastOnce()).getVmIpAddresses(vps4VmId);
    }

    @Test
    public void unbindsPublicIps() {
        command.execute(context, request);
        ArgumentCaptor<UnbindIp.Request> argument = ArgumentCaptor.forClass(UnbindIp.Request.class);
        verify(context).execute(startsWith("UnbindIP-"), eq(UnbindIp.class), argument.capture());
        UnbindIp.Request request = argument.getValue();
        assertEquals(hfsAddressId, request.hfsAddressId);
        assertEquals(true, request.forceIfVmInaccessible);
    }

    @Test
    public void getsAndRemovesCdnSites() {
        command.execute(context, request);
        ArgumentCaptor<Vps4RemoveCdnSite.Request> argument = ArgumentCaptor.forClass(Vps4RemoveCdnSite.Request.class);

        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vps4VmId);
        verify(context).execute(eq("RemoveCdnSite-" + vmCdnSite.siteId), eq(Vps4RemoveCdnSite.class), argument.capture());

        Vps4RemoveCdnSite.Request req = argument.getValue();
        assertEquals(vmCdnSite.siteId, req.siteId);
        assertEquals(vps4VmId, req.vmId);
        assertEquals(request.rebuildVmInfo.customerId, req.customerId);
    }

    @Test
    public void doesNotRemoveCdnSiteIfNullList() {
        when(cdnDataService.getActiveCdnSitesOfVm(vps4VmId)).thenReturn(null);
        command.execute(context, request);

        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vps4VmId);
        verify(context, times(0)).execute(startsWith("RemoveCdnSite-"), eq(Vps4RemoveCdnSite.class), any());
    }

    @Test
    public void doesNotRemoveCdnSiteIfEmptyList() {
        when(cdnDataService.getActiveCdnSitesOfVm(vps4VmId)).thenReturn(Collections.emptyList());
        command.execute(context, request);

        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vps4VmId);
        verify(context, times(0)).execute(startsWith("RemoveCdnSite-"), eq(Vps4RemoveCdnSite.class), any());
    }

    @Test
    public void deletesOriginalVm() {
        command.execute(context, request);
        ArgumentCaptor<DestroyVm.Request> argument = ArgumentCaptor.forClass(DestroyVm.Request.class);
        verify(context).execute(eq("DestroyVmHfs"), eq(DestroyVm.class), argument.capture());
        DestroyVm.Request destroyRequest = argument.getValue();
        assertEquals(originalHfsVmId, destroyRequest.hfsVmId);
        assertEquals(request.actionId, destroyRequest.actionId);
    }

    @Test
    public void deletesOldVmUsersFromDatabase() {
        command.execute(context, request);
        verify(vmUserService).deleteUser("customer", vps4VmId);
        verify(vmUserService).deleteUser("support-123", vps4VmId);
    }

    @Test
    public void createsNewVm() {
        command.execute(context, request);
        ArgumentCaptor<CreateVm.Request> argument = ArgumentCaptor.forClass(CreateVm.Request.class);
        verify(context).execute(eq("CreateVm"), eq(CreateVm.class), argument.capture());
        CreateVm.Request request = argument.getValue();
        assertEquals("hfs-centos-7-cpanel-11", request.image_name);
        assertEquals("raw-flavor", request.rawFlavor);
        assertEquals("user", request.username);
    }

    @Test
    public void updatesVmRecordOnCreate() {
        command.execute(context, request);
        verify(virtualMachineService).addHfsVmIdToVirtualMachine(vps4VmId, action.vmId);

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("name", "server-name");
        expectedParams.put("image_id", 7L);
        verify(virtualMachineService).updateVirtualMachine(vps4VmId, expectedParams);
    }

    @Test
    public void waitsForAndRecordsVmAction() {
        command.execute(context, request);
        verify(context).execute(WaitForAndRecordVmAction.class, action);
    }

    @Test
    public void bindsPublicIps() {
        command.execute(context, request);
        ArgumentCaptor<BindIp.Request> argument = ArgumentCaptor.forClass(BindIp.Request.class);
        verify(context).execute(startsWith("BindIP-"), eq(BindIp.class), argument.capture());
        BindIp.Request request = argument.getValue();
        assertEquals(newHfsVmId, request.hfsVmId);
        assertEquals(hfsAddressId, request.hfsAddressId);
        assertEquals(false, request.shouldForce);
    }

    @Test
    public void configuresCpanel() {
        command.execute(context, request);
        ArgumentCaptor<ConfigureCpanelRequest> argument = ArgumentCaptor.forClass(ConfigureCpanelRequest.class);
        verify(context).execute(eq(ConfigureCpanel.class), argument.capture());
        ConfigureCpanelRequest request = argument.getValue();
        assertEquals(newHfsVmId, request.vmId);
    }

    @Test
    public void configuresPleskPanel() {
        request.rebuildVmInfo.image.controlPanel = ControlPanel.PLESK;
        command.execute(context, request);

        ArgumentCaptor<ConfigurePleskRequest> argument = ArgumentCaptor.forClass(ConfigurePleskRequest.class);
        verify(context).execute(eq(ConfigurePlesk.class), argument.capture());
        ConfigurePleskRequest request = argument.getValue();
        assertEquals(newHfsVmId, request.vmId);
        assertEquals("user", request.username);
        assertNotNull(request.encryptedPassword);
    }

    @Test
    public void configuresNoControlPanel() {
        request.rebuildVmInfo.image.controlPanel = ControlPanel.MYH;
        command.execute(context, request);
        verify(context, never()).execute(eq(ConfigureCpanel.class), any());
        verify(context, never()).execute(eq(ConfigurePlesk.class), any());
    }

    @Test
    public void enablesAdminIfNoControlPanel() {
        when(virtualMachineService.hasControlPanel(vps4VmId)).thenReturn(false);
        command.execute(context, request);

        ArgumentCaptor<ToggleAdmin.Request> argument = ArgumentCaptor.forClass(ToggleAdmin.Request.class);
        verify(context).execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), argument.capture());
        ToggleAdmin.Request request = argument.getValue();
        assertEquals(newHfsVmId, request.vmId);
        assertEquals("user", request.username);
        assertTrue(request.enabled);
    }

    @Test
    public void disablesAdminIfControlPanel() {
        when(virtualMachineService.hasControlPanel(vps4VmId)).thenReturn(true);
        command.execute(context, request);

        verify(context, never()).execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), anyObject());
    }

    @Test
    public void createsVmUserInDb() {
        when(virtualMachineService.hasControlPanel(vps4VmId)).thenReturn(true);
        command.execute(context, request);
        verify(vmUserService).createUser("user", vps4VmId, false);
    }

    @Test
    public void setsRootPasswordForLinux() {
        when(virtualMachineService.isLinux(vps4VmId)).thenReturn(true);
        command.execute(context, request);

        ArgumentCaptor<SetPassword.Request> argument = ArgumentCaptor.forClass(SetPassword.Request.class);
        verify(context).execute(eq("SetRootUserPassword"), eq(SetPassword.class), argument.capture());
        SetPassword.Request request = argument.getValue();
        assertEquals(newHfsVmId, request.hfsVmId);
        assertEquals("cpanel", request.controlPanel);
        assertEquals(Arrays.asList("root"), request.usernames);
        assertNotNull(request.encryptedPassword);
    }

    @Test
    public void doesNotSetRootPasswordForWindows() {
        when(virtualMachineService.isLinux(vps4VmId)).thenReturn(false);
        command.execute(context, request);
        verify(context, never()).execute(eq("SetRootUserPassword"), eq(SetPassword.class), any());
    }

    @Test
    public void configuresMailRelay() {
        command.execute(context, request);
        ArgumentCaptor<ConfigureMailRelayRequest> argument = ArgumentCaptor.forClass(ConfigureMailRelayRequest.class);
        verify(context).execute(eq(ConfigureMailRelay.class), argument.capture());
        ConfigureMailRelayRequest request = argument.getValue();
        assertEquals(newHfsVmId, request.vmId);
        assertEquals("cpanel", request.controlPanel);
    }

    @Test
    public void testSendSetupEmail() throws Exception {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(SendSetupCompletedEmail.class), setupEmailCaptor.capture());
        SetupCompletedEmailRequest capturedRequest = setupEmailCaptor.getValue();
        assertEquals(vm.name, capturedRequest.serverName);
        assertEquals(vm.primaryIpAddress.ipAddress, capturedRequest.ipAddress);
        assertEquals(orionGuid, capturedRequest.orionGuid);
        assertEquals(shopperId, capturedRequest.shopperId);
        assertFalse(capturedRequest.isManaged);
    }

    @Test
    public void testSendSetupEmailDoesNotThrowException() throws Exception {
        when(sendSetupCompletedEmail.execute(eq(context), any(SetupCompletedEmailRequest.class)))
                .thenThrow(new RuntimeException("SendMessageFailed"));
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(SendSetupCompletedEmail.class), setupEmailCaptor.capture());
    }

    @Test
    public void setsEcommCommonName() {
        command.execute(context, request);
        verify(creditService).setCommonName(orionGuid, "server-name");
    }

    @Test
    public void configuresMonitoringIfHasPanopta() {
        PanoptaServerDetails serverDetails = mock(PanoptaServerDetails.class);
        when(panoptaDataService.getPanoptaServerDetails(vps4VmId)).thenReturn(serverDetails);
        command.execute(context, request);
        ArgumentCaptor<SetupPanopta.Request> argument = ArgumentCaptor.forClass(SetupPanopta.Request.class);
        verify(context).execute(eq(SetupPanopta.class), argument.capture());
        SetupPanopta.Request request = argument.getValue();
        assertEquals(newHfsVmId, request.hfsVmId);
        assertEquals(orionGuid, request.orionGuid);
        assertEquals(vps4VmId, request.vmId);
        assertEquals(shopperId, request.shopperId);
        assertEquals(fqdn, request.fqdn);
    }

    @Test
    public void skipsMonitoringSetupIfNoPanoptaDetails() {
        when(panoptaDataService.getPanoptaServerDetails(vps4VmId)).thenReturn(null);
        command.execute(context, request);
        verify(context, never()).execute(eq(SetupPanopta.class), any(SetupPanopta.Request.class));
    }

    @Test
    public void updateHfsVmTrackingRecord() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("UpdateHfsVmTrackingRecord"),
                any(Function.class), eq(Void.class));
    }

    @Test
    public void doesNotReleaseIpsIfRequested() {
        command.execute(context, request);
        verify(context, never()).execute(startsWith("RemoveIp-"), eq(BindIp.class), anyObject());
    }

    @Test
    public void doesNotDeleteIpsinDBIfRequested() {
        command.execute(context, request);
        verify(context, never()).execute(startsWith("MarkIpDeleted-"), eq(BindIp.class), anyObject());
    }

    @Test
    public void releasesIpsIfRequested() {
        request.rebuildVmInfo.keepAdditionalIps = false;
        command.execute(context, request);
        verify(context, never()).execute(startsWith("RemoveIp-"), eq(BindIp.class), anyObject());
    }


    @Test
    public void deleteIpsinDBIfRequested() {
        request.rebuildVmInfo.keepAdditionalIps = false;
        command.execute(context, request);
        verify(context, never()).execute(startsWith("MarkIpDeleted-"), eq(BindIp.class), anyObject());
    }
}
