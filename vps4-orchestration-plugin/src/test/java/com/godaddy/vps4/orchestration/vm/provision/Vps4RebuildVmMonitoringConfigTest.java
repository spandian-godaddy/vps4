package com.godaddy.vps4.orchestration.vm.provision;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.panopta.CreatePanoptaCustomer;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.phase2.Vps4ExternalsModule;
import com.godaddy.vps4.orchestration.vm.Vps4RebuildVm;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.IpAddress;

@RunWith(MockitoJUnitRunner.class)
public class Vps4RebuildVmMonitoringConfigTest {

    private static Injector injector = Guice.createInjector(
            new Vps4ExternalsModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(CreditService.class).toInstance(mock(CreditService.class));
                    bind(VirtualMachineService.class).toInstance(mock(VirtualMachineService.class));
                    bind(ActionService.class).toInstance(mock(ActionService.class));
                    bind(VmUserService.class).toInstance(mock(VmUserService.class));
                    bind(NetworkService.class).toInstance(mock(NetworkService.class));
                    bind(VirtualMachineCredit.class).toInstance(mock(VirtualMachineCredit.class));
                    bind(PanoptaDataService.class).toInstance(mock(PanoptaDataService.class));
                    bind(PanoptaCustomer.class).toInstance(mock(PanoptaCustomer.class));
                    bind(PanoptaCustomerDetails.class).toInstance(mock(PanoptaCustomerDetails.class));
                    bind(PanoptaServerDetails.class).toInstance(mock(PanoptaServerDetails.class));
                    bind(CreatePanoptaCustomer.class).toInstance(mock(CreatePanoptaCustomer.class));
                    bind(CreatePanoptaCustomer.Response.class).toInstance(mock(CreatePanoptaCustomer.Response.class));
                    bind(Config.class).toInstance(mock(Config.class));
                }
            });


    private Vps4RebuildVm command;
    private CommandContext context;
    private Vps4RebuildVm.Request request;
    private VmAction vmAction;
    private UUID vps4VmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private IpAddress primaryIp;
    private VirtualMachine vm;
    private long hfsVmId = 6789;
    private long newHfsVmId = 456L;
    private Image image;
    private static final String fakeServerKey = "ultra-fake-server-key";
    private static final String fakeCustomerKey = "super-fake-customer-key";
    private static final String fakeShopperId = "mega-fake-shopper-id";
    private DataCenter dummyDataCenter;

    @Inject private Config config;
    @Inject private PanoptaCustomer panoptaCustomer;
    @Inject private PanoptaCustomerDetails panoptaCustomerDetails;
    @Inject private PanoptaServerDetails panoptaServerDetails;
    @Inject private CreatePanoptaCustomer.Response createCustomerResponse;
    @Inject private CreditService creditService;
    @Inject private VirtualMachineCredit credit;
    @Inject private VirtualMachineService virtualMachineService;

    @Before
    public void setUpTest() {
        injector.injectMembers(this);

        command = injector.getInstance(Vps4RebuildVm.class);

        primaryIp = new IpAddress();
        primaryIp.address = "1.2.3.4";

        image = new Image();
        image.operatingSystem = Image.OperatingSystem.LINUX;
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar";

        vm = new VirtualMachine(UUID.randomUUID(), hfsVmId, UUID.randomUUID(), 1,
                                null, "fake_server",
                                image, null, null, null, null,
                                "fake.host.name", 0, UUID.randomUUID());

        dummyDataCenter = new DataCenter();
        dummyDataCenter.dataCenterName = "iad2";
        dummyDataCenter.dataCenterId = 2;

        vmAction = new com.godaddy.hfs.vm.VmAction();
        vmAction.vmActionId = 777L;
        vmAction.vmId = newHfsVmId;

        context = setupMockContext();
        request = setupProvisionRequest();
        setupPanoptaRebuildVmMocks();
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);

        when(mockContext.getId()).thenReturn(UUID.randomUUID());
        when(mockContext.execute(eq("GetHfsVmId"), any(Function.class), eq(long.class))).thenReturn(hfsVmId);
        when(mockContext.execute(eq("CreateVm"), eq(CreateVm.class), any(CreateVm.Request.class)))
                .thenReturn(vmAction);
        when(mockContext.execute(eq(AllocateIp.class), any(AllocateIp.Request.class))).thenReturn(primaryIp);
        when(mockContext.execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class))).thenReturn(createCustomerResponse);
        return mockContext;
    }

    private Vps4RebuildVm.Request setupProvisionRequest() {
        request = new Vps4RebuildVm.Request();
        request.rebuildVmInfo = new RebuildVmInfo();
        request.rebuildVmInfo.vmId = vps4VmId;
        request.rebuildVmInfo.orionGuid = orionGuid;
        request.rebuildVmInfo.image = new Image();
        request.rebuildVmInfo.image.hfsName = "hfs-centos-7-cpanel-11";
        request.rebuildVmInfo.image.imageId = 7L;
        request.rebuildVmInfo.image.controlPanel = Image.ControlPanel.CPANEL;
        request.rebuildVmInfo.rawFlavor = "raw-flavor";
        request.rebuildVmInfo.username = "user";
        request.rebuildVmInfo.serverName = "server-name";
        request.rebuildVmInfo.hostname = "host.name";
        request.rebuildVmInfo.encryptedPassword = "encrypted".getBytes();

        vm.image = new Image();
        vm.image.controlPanel = Image.ControlPanel.CPANEL;
        when(virtualMachineService.getVirtualMachine(vps4VmId)).thenReturn(vm);

        return request;
    }

    private void setupPanoptaRebuildVmMocks() {
        when(config.get(eq("panopta.installation.enabled"), eq("false"))).thenReturn("true");
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(fakeCustomerKey);
        when(panoptaServerDetails.getServerKey()).thenReturn(fakeServerKey);
        when(credit.getDataCenter()).thenReturn(dummyDataCenter);
        when(config.get(eq("panopta.api.templates.webhook"))).thenReturn("fake-datacenter-template-id");
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(credit.getOperatingSystem()).thenReturn("linux");
        when(credit.hasMonitoring()).thenReturn(true);
        when(credit.getShopperId()).thenReturn(fakeShopperId);
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
        when(createCustomerResponse.getPanoptaCustomer()).thenReturn(panoptaCustomer);
    }

    @Test
    public void configuresPanoptaOnRebuildForFullyManaged() throws Exception {
        when(config.get(eq("panopta.api.templates.FULLY_MANAGED.linux"))).thenReturn("fake-template-id");
        when(credit.effectiveManagedLevel()).thenReturn(VirtualMachineCredit.EffectiveManagedLevel.FULLY_MANAGED);

        command.executeWithAction(context, request);

        ArgumentCaptor<SetupPanopta.Request> argumentCaptor = ArgumentCaptor.forClass(SetupPanopta.Request.class);
        verify(context).execute(eq(SetupPanopta.class), argumentCaptor.capture());
        SetupPanopta.Request setupPanoptaRequest = argumentCaptor.getValue();
        assertEquals(newHfsVmId, setupPanoptaRequest.hfsVmId);
        assertEquals(orionGuid, setupPanoptaRequest.orionGuid);
        assertEquals(vps4VmId, setupPanoptaRequest.vmId);
        assertEquals(fakeShopperId, setupPanoptaRequest.shopperId);
    }

    @Test
    public void configuresPanoptaOnRebuildForManagedV1() throws Exception {
        when(config.get(eq("panopta.api.templates.MANAGED_V1.linux"))).thenReturn("fake-template-id");
        when(credit.effectiveManagedLevel()).thenReturn(VirtualMachineCredit.EffectiveManagedLevel.MANAGED_V1);

        command.executeWithAction(context, request);

        ArgumentCaptor<SetupPanopta.Request> argumentCaptor = ArgumentCaptor.forClass(SetupPanopta.Request.class);
        verify(context).execute(eq(SetupPanopta.class), argumentCaptor.capture());
        SetupPanopta.Request setupPanoptaRequest = argumentCaptor.getValue();
        assertEquals(newHfsVmId, setupPanoptaRequest.hfsVmId);
        assertEquals(orionGuid, setupPanoptaRequest.orionGuid);
        assertEquals(vps4VmId, setupPanoptaRequest.vmId);
        assertEquals(fakeShopperId, setupPanoptaRequest.shopperId);
    }

    @Test
    public void configuresPanoptaOnRebuildForManagedV2() throws Exception {
        when(config.get(eq("panopta.api.templates.MANAGED_V2.linux"))).thenReturn("fake-template-id");
        when(credit.effectiveManagedLevel()).thenReturn(VirtualMachineCredit.EffectiveManagedLevel.MANAGED_V2);

        command.executeWithAction(context, request);

        ArgumentCaptor<SetupPanopta.Request> argumentCaptor = ArgumentCaptor.forClass(SetupPanopta.Request.class);
        verify(context).execute(eq(SetupPanopta.class), argumentCaptor.capture());
        SetupPanopta.Request setupPanoptaRequest = argumentCaptor.getValue();
        assertEquals(newHfsVmId, setupPanoptaRequest.hfsVmId);
        assertEquals(orionGuid, setupPanoptaRequest.orionGuid);
        assertEquals(vps4VmId, setupPanoptaRequest.vmId);
        assertEquals(fakeShopperId, setupPanoptaRequest.shopperId);
    }


    @Test
    public void configuresPanoptaOnRebuildForSelfManagedV2() throws Exception {
        when(config.get(eq("panopta.api.templates.SELF_MANAGED_V2.linux"))).thenReturn("fake-template-id");
        when(credit.effectiveManagedLevel()).thenReturn(VirtualMachineCredit.EffectiveManagedLevel.SELF_MANAGED_V2);

        command.executeWithAction(context, request);

        ArgumentCaptor<SetupPanopta.Request> argumentCaptor = ArgumentCaptor.forClass(SetupPanopta.Request.class);
        verify(context).execute(eq(SetupPanopta.class), argumentCaptor.capture());
        SetupPanopta.Request setupPanoptaRequest = argumentCaptor.getValue();
        assertEquals(newHfsVmId, setupPanoptaRequest.hfsVmId);
        assertEquals(orionGuid, setupPanoptaRequest.orionGuid);
        assertEquals(vps4VmId, setupPanoptaRequest.vmId);
        assertEquals(fakeShopperId, setupPanoptaRequest.shopperId);
    }

    @Test
    public void doesNotInstallPanoptaForExistingSelfManagedV1() throws Exception {
        when(config.get(eq("panopta.api.templates.SELF_MANAGED_V1.linux"))).thenReturn("fake-template-id");
        when(credit.effectiveManagedLevel()).thenReturn(VirtualMachineCredit.EffectiveManagedLevel.SELF_MANAGED_V1);

        command.executeWithAction(context, request);

        verify(context, never()).execute(eq(SetupPanopta.class), any());
        verify(creditService, never()).setPanoptaInstalled(eq(orionGuid), eq(true));
    }

}
