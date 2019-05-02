package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
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

    UUID vps4VmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    long originalHfsVmId = 123L;
    long newHfsVmId = 456L;
    long actionId = 12L;
    long ipAddressId = 34L;
    VirtualMachine vm;
    VmAction action;

    UnbindIp unbindIp = mock(UnbindIp.class);
    DestroyVm destroyVm = mock(DestroyVm.class);
    CreateVm createVm = mock(CreateVm.class);
    WaitForAndRecordVmAction waitForVm = mock(WaitForAndRecordVmAction.class);
    BindIp bindIp = mock(BindIp.class);
    ConfigureCpanel configCpanel = mock(ConfigureCpanel.class);
    ConfigurePlesk configPlesk = mock(ConfigurePlesk.class);
    SetHostname setHostname = mock(SetHostname.class);
    ToggleAdmin enableAdmin = mock(ToggleAdmin.class);
    SetPassword setPassword = mock(SetPassword.class);
    ConfigureMailRelay setMailRelay = mock(ConfigureMailRelay.class);


    Vps4RebuildVm.Request request;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UnbindIp.class).toInstance(unbindIp);
        binder.bind(DestroyVm.class).toInstance(destroyVm);
        binder.bind(CreateVm.class).toInstance(createVm);
        binder.bind(WaitForAndRecordVmAction.class).toInstance(waitForVm);
        binder.bind(BindIp.class).toInstance(bindIp);
        binder.bind(ConfigureCpanel.class).toInstance(configCpanel);
        binder.bind(ConfigurePlesk.class).toInstance(configPlesk);
        binder.bind(SetHostname.class).toInstance(setHostname);
        binder.bind(ToggleAdmin.class).toInstance(enableAdmin);
        binder.bind(SetPassword.class).toInstance(setPassword);
        binder.bind(ConfigureMailRelay.class).toInstance(setMailRelay);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    Vps4RebuildVm command = new Vps4RebuildVm(actionService, virtualMachineService,
            vps4NetworkService, vmUserService, creditService);

    @Before
    public void setupTest() {
        request = new Vps4RebuildVm.Request();
        request.rebuildVmInfo = new RebuildVmInfo();
        request.rebuildVmInfo.vmId = vps4VmId;
        request.rebuildVmInfo.orionGuid = orionGuid;
        request.rebuildVmInfo.image = new Image();
        request.rebuildVmInfo.image.hfsName = "hfs-centos-7-cpanel-11";
        request.rebuildVmInfo.image.imageId = 7L;
        request.rebuildVmInfo.image.controlPanel = ControlPanel.CPANEL;
        request.rebuildVmInfo.rawFlavor = "raw-flavor";
        request.rebuildVmInfo.username = "user";
        request.rebuildVmInfo.serverName = "server-name";
        request.rebuildVmInfo.hostname = "host.name";
        request.rebuildVmInfo.encryptedPassword = "encrypted".getBytes();

        IpAddress publicIp = new IpAddress();
        publicIp.ipAddressId = ipAddressId;
        when(vps4NetworkService.getVmIpAddresses(vps4VmId)).thenReturn(Arrays.asList(publicIp));

        vm = new VirtualMachine();
        vm.hfsVmId = originalHfsVmId;
        vm.image = new Image();
        vm.image.controlPanel = ControlPanel.CPANEL;
        when(virtualMachineService.getVirtualMachine(vps4VmId)).thenReturn(vm);

        VmUser customerUser = new VmUser("customer", vps4VmId, false, VmUserType.CUSTOMER);
        VmUser supportUser = new VmUser("support-123", vps4VmId, true, VmUserType.SUPPORT);
        when(vmUserService.listUsers(vps4VmId)).thenReturn(Arrays.asList(customerUser, supportUser));

        action = new VmAction();
        action.vmId = newHfsVmId;
        doReturn(action).when(context).execute(eq("CreateVm"), eq(CreateVm.class), any());
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
        assertEquals(ipAddressId, request.addressId);
        assertEquals(true, request.forceIfVmInaccessible);
    }

    @Test
    public void deletesOriginalVm() {
        command.execute(context, request);
        verify(context).execute("DestroyVmHfs", DestroyVm.class, originalHfsVmId);
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
        assertEquals(ipAddressId, request.addressId);
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
        vm.image.controlPanel = ControlPanel.PLESK;
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
        vm.image.controlPanel = ControlPanel.MYH;
        command.execute(context, request);
        verify(context, never()).execute(eq(ConfigureCpanel.class), any());
        verify(context, never()).execute(eq(ConfigurePlesk.class), any());
    }

    @Test
    public void setsHostname() {
        command.execute(context, request);

        ArgumentCaptor<SetHostname.Request> argument = ArgumentCaptor.forClass(SetHostname.Request.class);
        verify(context).execute(eq(SetHostname.class), argument.capture());
        SetHostname.Request request = argument.getValue();
        assertEquals(newHfsVmId, request.hfsVmId);
        assertEquals("host.name", request.hostname);
        assertEquals("cpanel", request.controlPanel);
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
        assertEquals(true, request.enabled);
    }

    @Test
    public void doesNotEnableAdminIfControlPanel() {
        when(virtualMachineService.hasControlPanel(vps4VmId)).thenReturn(true);
        command.execute(context, request);
        verify(context, never()).execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), any());
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
    public void setsEcommCommonName() {
        command.execute(context, request);
        verify(creditService).setCommonName(orionGuid, "server-name");
    }

}
