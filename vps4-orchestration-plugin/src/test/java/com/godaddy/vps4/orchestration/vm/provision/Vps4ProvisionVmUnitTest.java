package com.godaddy.vps4.orchestration.vm.provision;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.messaging.DefaultVps4MessagingService;
import com.godaddy.vps4.messaging.MessagingModule;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.monitoring.MonitoringModule;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.phase2.Vps4ExternalsModule;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.NodePingService;

@RunWith(MockitoJUnitRunner.class)
public class Vps4ProvisionVmUnitTest {
    static Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new MessagingModule(),
            new MonitoringModule(),
            new Vps4ExternalsModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(CreditService.class).toInstance(mock(CreditService.class));
                    bind(NodePingService.class).toInstance(mock(NodePingService.class));
                    bind(VirtualMachineService.class).toInstance(mock(VirtualMachineService.class));
                    bind(ActionService.class).toInstance(mock(ActionService.class));
                    bind(VmUserService.class).toInstance(mock(VmUserService.class));
                    bind(NetworkService.class).toInstance(mock(NetworkService.class));
                    bind(DefaultVps4MessagingService.class).toInstance(mock(DefaultVps4MessagingService.class));
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(HfsVmTrackingRecordService.class).toInstance(mock(HfsVmTrackingRecordService.class));
                }
            }
    );
    private Vps4ProvisionVm command;
    private CommandContext context;
    private ProvisionRequest request;
    private VmAction vmAction;
    private UUID vps4VmId = UUID.randomUUID();
    private IpAddress primaryIp;
    private VirtualMachine vm;
    private long hfsVmId = 6789;
    private Image image;
    private static final String username = "fake_user";

    @Inject private VirtualMachineService virtualMachineService;
    @Inject private Vps4MessagingService messagingService;

    @Captor private ArgumentCaptor<CreateVm.Request> createVmRequestArgumentCaptor;

    @BeforeClass
    public static void newInjector() {
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);

        command = injector.getInstance(Vps4ProvisionVm.class);

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

        context = setupMockContext();
        request = setupProvisionRequest();
    }

    @After
    public void teardownTest() {
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);

        when(mockContext.getId()).thenReturn(UUID.randomUUID());
        when(mockContext.execute(eq("AddBackupJobIdToVM"), any(Function.class), eq(Void.class))).thenReturn(null);
        when(mockContext.execute(eq(ConfigurePlesk.class), any())).thenReturn(null);
        when(mockContext.execute(eq(ConfigureCpanel.class), any())).thenReturn(null);
        when(mockContext.execute(eq(BindIp.class), any())).thenReturn(null);
        when(mockContext.execute(eq(ConfigureMailRelay.class), any())).thenReturn(null);
        when(mockContext.execute(eq("CreateVps4User"), any(Function.class), eq(Void.class))).thenReturn(null);
        when(mockContext.execute(eq(SetPassword.class), any())).thenReturn(null);
        when(mockContext.execute(eq("Vps4ProvisionVm"), any(Function.class), eq(Void.class))).thenReturn(null);
        when(mockContext.execute(eq(AllocateIp.class), any(AllocateIp.Request.class))).thenReturn(primaryIp);
        when(mockContext.execute(startsWith("UnbindIP-"), eq(UnbindIp.class), any())).thenReturn(null);
        when(mockContext.execute(startsWith("BindIP-"), eq(BindIp.class), any())).thenReturn(null);
        when(mockContext.execute(eq("SetCommonName"), any(Function.class))).thenReturn(null);
        return mockContext;
    }

    private Image setupImage() {
        Image image = new Image();
        image.operatingSystem = Image.OperatingSystem.LINUX;
        image.hfsName = "hfs-debian-8";
        image.controlPanel = Image.ControlPanel.MYH;
        image.imageId = 13;
        return image;
    }

    private ProvisionRequest setupProvisionRequest() {
        ProvisionRequest provisionRequest = new ProvisionRequest();
        provisionRequest.rawFlavor = "";
        provisionRequest.sgid = "";
        provisionRequest.image_name = "";
        provisionRequest.username = username;
        provisionRequest.encryptedPassword = "sweeTT3st!".getBytes();
        provisionRequest.zone = null;
        provisionRequest.actionId = 12;
        provisionRequest.vmInfo = setupVmInfo();
        provisionRequest.shopperId = UUID.randomUUID().toString();
        provisionRequest.serverName = "fake_server_name";
        provisionRequest.orionGuid = UUID.randomUUID();
        provisionRequest.privateLabelId = "1";
        return provisionRequest;
    }

    private ProvisionVmInfo setupVmInfo() {
        ProvisionVmInfo vmInfo = new ProvisionVmInfo();
        vmInfo.vmId = vps4VmId;
        vmInfo.image = setupImage();
        vmInfo.mailRelayQuota = 5000;
        vmInfo.hasMonitoring = false;
        vmInfo.sgid = "fake_sgid";
        vmInfo.diskGib = new Random().nextInt(100);
        vmInfo.managedLevel = 0;
        return vmInfo;
    }

    @Test
    public void createVmRequestHasPrivateLabelId() {
        vmAction = mock(VmAction.class);
        when(virtualMachineService.getVirtualMachine(any())).thenReturn(vm);
        when(messagingService.sendSetupEmail(anyString(), anyString(), anyString(),
                anyString(), anyBoolean())).thenReturn("email_id");
        when(context.execute(eq(CreateVm.class), any(CreateVm.Request.class)))
                .thenReturn(vmAction);

        command.execute(context, request);

        verify(context, atLeastOnce()).execute(eq(CreateVm.class), createVmRequestArgumentCaptor.capture());
        Assert.assertEquals("1", createVmRequestArgumentCaptor.getValue().privateLabelId);
    }
}