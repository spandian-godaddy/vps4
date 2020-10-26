package com.godaddy.vps4.orchestration.vm.rebuild;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildDedicated;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.panopta.PanoptaDataService;
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

public class Vps4RebuildDedicatedTest {

    ActionService actionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService vps4NetworkService = mock(NetworkService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    CreditService creditService = mock(CreditService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);

    UUID vps4VmId = UUID.randomUUID();
    long hfsVmId = 123L;
    long ipAddressId = 34L;
    String fqdn = "10.0.0.1";
    VirtualMachine vm;
    VmAction action;

    UnbindIp unbindIp = mock(UnbindIp.class);
    RebuildDedicated rebuildDedicated = mock(RebuildDedicated.class);
    WaitForAndRecordVmAction waitForVm = mock(WaitForAndRecordVmAction.class);
    BindIp bindIp = mock(BindIp.class);
    ConfigureCpanel configCpanel = mock(ConfigureCpanel.class);
    ConfigurePlesk configPlesk = mock(ConfigurePlesk.class);
    ToggleAdmin enableAdmin = mock(ToggleAdmin.class);
    SetPassword setPassword = mock(SetPassword.class);
    ConfigureMailRelay setMailRelay = mock(ConfigureMailRelay.class);
    SetupPanopta setupPanopta = mock(SetupPanopta.class);

    Vps4RebuildVm.Request request;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UnbindIp.class).toInstance(unbindIp);
        binder.bind(RebuildDedicated.class).toInstance(rebuildDedicated);
        binder.bind(WaitForAndRecordVmAction.class).toInstance(waitForVm);
        binder.bind(BindIp.class).toInstance(bindIp);
        binder.bind(ConfigureCpanel.class).toInstance(configCpanel);
        binder.bind(ConfigurePlesk.class).toInstance(configPlesk);
        binder.bind(ToggleAdmin.class).toInstance(enableAdmin);
        binder.bind(SetPassword.class).toInstance(setPassword);
        binder.bind(ConfigureMailRelay.class).toInstance(setMailRelay);
        binder.bind(SetupPanopta.class).toInstance(setupPanopta);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    Vps4RebuildVm command = new Vps4RebuildDedicated(actionService, virtualMachineService, vps4NetworkService,
            vmUserService, creditService, panoptaDataService, hfsVmTrackingRecordService);

    @Before
    public void setupTest() {
        request = new Vps4RebuildVm.Request();
        request.rebuildVmInfo = new RebuildVmInfo();
        request.rebuildVmInfo.vmId = vps4VmId;
        request.rebuildVmInfo.image = new Image();
        request.rebuildVmInfo.image.hfsName = "centos7-cpanel-latest_64";
        request.rebuildVmInfo.image.imageId = 7L;
        request.rebuildVmInfo.image.controlPanel = ControlPanel.CPANEL;
        request.rebuildVmInfo.username = "user";
        request.rebuildVmInfo.serverName = "server-name";
        request.rebuildVmInfo.hostname = "host.name";

        IpAddress publicIp = new IpAddress();
        publicIp.ipAddressId = ipAddressId;
        publicIp.ipAddress = fqdn;
        when(vps4NetworkService.getVmIpAddresses(vps4VmId)).thenReturn(Arrays.asList(publicIp));
        when(vps4NetworkService.getVmPrimaryAddress(vps4VmId)).thenReturn(publicIp);

        vm = new VirtualMachine();
        vm.hfsVmId = hfsVmId;
        vm.image = new Image();
        vm.image.controlPanel = ControlPanel.CPANEL;
        when(virtualMachineService.getVirtualMachine(vps4VmId)).thenReturn(vm);

        VmUser customerUser = new VmUser("customer", vps4VmId, false, VmUserType.CUSTOMER);
        VmUser supportUser = new VmUser("support-123", vps4VmId, true, VmUserType.SUPPORT);
        when(vmUserService.listUsers(vps4VmId)).thenReturn(Arrays.asList(customerUser, supportUser));

        action = new VmAction();
        action.vmId = hfsVmId;
        doReturn(action).when(context).execute(eq("RebuildDedicated"), eq(RebuildDedicated.class), any());
    }

    @Test
    public void rebuildsVmInHfs() {
        command.execute(context, request);
        ArgumentCaptor<RebuildDedicated.Request> argument = ArgumentCaptor.forClass(RebuildDedicated.Request.class);
        verify(context).execute(eq("RebuildDedicated"), eq(RebuildDedicated.class), argument.capture());
        RebuildDedicated.Request request = argument.getValue();
        assertEquals(hfsVmId, request.vmId);
        assertEquals("host.name", request.hostname);
        assertEquals("centos7-cpanel-latest_64", request.image_name);
        assertEquals("user", request.username);
    }

    @Test
    public void updatesVmRecordOnCreate() {
        command.execute(context, request);
        verify(virtualMachineService, never()).addHfsVmIdToVirtualMachine(vps4VmId, action.vmId);

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
    public void updateHfsVmTrackingRecord() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("UpdateHfsVmTrackingRecord"), any(Function.class), eq(Void.class));
    }

    @Test
    public void doesNotUnbindPublicIps() {
        command.execute(context, request);
        verify(context, never()).execute(startsWith("UnbindIP-"), eq(UnbindIp.class), anyObject());
    }

    @Test
    public void doesNotBindPublicIps() {
        command.execute(context, request);
        verify(context, never()).execute(startsWith("BindIP-"), eq(BindIp.class), anyObject());
    }

    @Test
    public void doesNotConfiguresMailRelay() {
        command.execute(context, request);
        verify(context, never()).execute(eq(ConfigureMailRelay.class), anyObject());
    }
}
