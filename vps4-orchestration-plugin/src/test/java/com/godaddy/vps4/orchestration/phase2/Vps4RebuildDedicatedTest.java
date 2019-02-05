package com.godaddy.vps4.orchestration.phase2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildDedicated;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.vm.Vps4RebuildDedicated;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Vps4RebuildDedicatedTest {
    static Injector injector;
    private Vps4RebuildDedicated command;
    private CommandContext context;
    private Vps4RebuildDedicated.Request request;
    private VmAction vmAction;
    private UUID vps4VmId;
    private long rebuildActionId;
    private Project vps4Project;
    private List<IpAddress> ipAddresses;
    private long hfsNewVmId = 6789;
    private Vm hfsVm;
    private VirtualMachine vps4Vm, vps4NewVm;
    private static final String username = "fake_user";
    private static final String password = "P@$$w0rd1";

    @Inject private Vps4UserService vps4UserService;
    @Inject private ProjectService projectService;
    @Inject private VmService hfsVmService;
    @Inject private VirtualMachineService vps4VmService;
    @Inject private ActionService actionService;
    @Inject private CreditService creditService;
    @Inject private VmUserService vmUserService;

    VirtualMachineService spyVps4VmService;
    VmUserService spyVmUserService;

    @Captor private ArgumentCaptor<Function<CommandContext, Long>> getHfsVmIdLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Vm>> getHfsVmLambdaCaptor;
    @Captor private ArgumentCaptor<AddUser.Request> addUserToServerArgumentCaptor;
    @Captor private ArgumentCaptor<SetPassword.Request> setPasswordArgumentCaptor;
    @Captor private ArgumentCaptor<ToggleAdmin.Request> toggleAdminArgumentCaptor;
    @Captor private ArgumentCaptor<ConfigureMailRelay.ConfigureMailRelayRequest> configMTAArgumentCaptor;
    @Captor private ArgumentCaptor<ConfigureCpanel.ConfigureCpanelRequest> configureCpanelRequestArgumentCaptor;
    @Captor private ArgumentCaptor<ConfigurePlesk.ConfigurePleskRequest> configurePleskRequestArgumentCaptor;
    @Captor private ArgumentCaptor<RebuildDedicated.Request> rebuildDedRequestArgCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new DatabaseModule(),
                new SecurityModule(),
                new VmModule(),
                new Vps4ExternalsModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                    }

                    @Provides
                    public CreditService createMockCreditService() {
                        return mock(CreditService.class);
                    }
                }
        );
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);

        spyVps4VmService = spy(vps4VmService);
        spyVmUserService = spy(vmUserService);

        command = new Vps4RebuildDedicated(actionService, hfsVmService, spyVps4VmService,
                spyVmUserService, creditService);
        addTestSqlData();

        vps4NewVm = mock(VirtualMachine.class);
        when(spyVps4VmService.getVirtualMachine(anyLong())).thenReturn(vps4NewVm);
        vps4NewVm.image = setupImage();
        vps4NewVm.hfsVmId = hfsNewVmId;

        context = setupMockContext();
        request = getCommandRequest();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(
                injector.getInstance(DataSource.class), injector.getInstance(Vps4UserService.class));
    }

    private void addTestSqlData() {
        SqlTestData.insertUser(vps4UserService);
        vps4Project = SqlTestData.insertProject(projectService, vps4UserService);
        vps4Vm = SqlTestData.insertDedicatedVm(vps4VmService, vps4UserService); // ensure you create a dedicated vm
        vps4VmId = vps4Vm.vmId;
        rebuildActionId = SqlTestData.insertVmAction(actionService, vps4VmId, ActionType.REBUILD_VM);
        ipAddresses = new ArrayList<>();
        VmUser vmUser = new VmUser("fake_vm_user", vps4VmId, true, VmUserType.SUPPORT);
        SqlTestData.insertVmUser(vmUser, vmUserService);
        VmUser vmCustomer = new VmUser("fake_vm_customer", vps4VmId, true, VmUserType.CUSTOMER);
        SqlTestData.insertVmUser(vmCustomer, vmUserService);
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());

        when(mockContext.execute(eq("GetHfsVmId"), any(Function.class), eq(long.class))).thenReturn(SqlTestData.hfsVmId);
        when(mockContext.execute(eq("GetVirtualMachine"), any(Function.class), eq(VirtualMachine.class))).thenReturn(vps4Vm);
        when(mockContext.execute(eq("GetNocfoxImageId"), any(Function.class), eq(String.class))).thenReturn(SqlTestData.nfImageId);
        when(mockContext.execute(eq("GetVmOSDistro"), any(Function.class), eq(String.class))).thenReturn(SqlTestData.IMAGE_NAME);

        vmAction = mock(VmAction.class);
        when(mockContext.execute(eq("RebuildDedicated"), eq(RebuildDedicated.class), any(RebuildDedicated.Request.class)))
                .thenReturn(vmAction);
        vmAction.vmId = hfsNewVmId;

        hfsVm = new Vm();
        hfsVm.vmId = hfsNewVmId;

        when(mockContext.execute(eq("GetVmAfterCreate"), any(Function.class), eq(Vm.class))).thenReturn(hfsVm);

        when(mockContext.execute(eq("UpdateHfsVmId"), any(Function.class), eq(Void.class))).thenReturn(null);
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

    private Image setupPleskImage() {
        Image image = new Image();
        image.operatingSystem = Image.OperatingSystem.WINDOWS;
        image.hfsName = "hfs-windows-2016-plesk-17";
        image.controlPanel = Image.ControlPanel.PLESK;
        image.imageId = 11;
        return image;
    }

    private Image setupcPanelImage() {
        Image image = new Image();
        image.operatingSystem = Image.OperatingSystem.LINUX;
        image.hfsName = "hfs-centos-7-cpanel-11";
        image.controlPanel = Image.ControlPanel.CPANEL;
        image.imageId = 7;
        return image;
    }

    private Vps4RebuildDedicated.Request getCommandRequest() {
        Vps4RebuildDedicated.Request req = new Vps4RebuildDedicated.Request();
        req.actionId = rebuildActionId;
        req.rebuildVmInfo = new RebuildVmInfo();
        req.rebuildVmInfo.vmId = vps4VmId;
        req.rebuildVmInfo.hostname = "foobar";
        req.rebuildVmInfo.username = username;
        req.rebuildVmInfo.encryptedPassword = password.getBytes();
        req.rebuildVmInfo.zone = "ded-zone-1";
        req.rebuildVmInfo.rawFlavor = "rawflavor";
        req.rebuildVmInfo.image = setupImage();
        req.rebuildVmInfo.sgid = vps4Project.getVhfsSgid();
        req.rebuildVmInfo.serverName = SqlTestData.TEST_VM_NAME;
        req.rebuildVmInfo.privateLabelId = "1";
        return req;
    }

    @Test
    public void getsHfsVmIdOfTheCurrentVm() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(eq("GetHfsVmId"), getHfsVmIdLambdaCaptor.capture(), eq(long.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, Long> lambda = getHfsVmIdLambdaCaptor.getValue();
        long oldHfsVmId = lambda.apply(context);
        Assert.assertEquals(oldHfsVmId, SqlTestData.hfsVmId);
    }

    @Test
    public void rebuildsDedicatedVmWithRequestParameters() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(
                        eq("RebuildDedicated"), eq(RebuildDedicated.class),
                        rebuildDedRequestArgCaptor.capture()
                );

        RebuildDedicated.Request request = rebuildDedRequestArgCaptor.getValue();
        // TODO: add this assertion once HFS adds the ability to pass in the private label id for a dedicated rebuild.
        //  Assert.assertEquals("1", request.privateLabelId);
        Assert.assertEquals(SqlTestData.hfsVmId, request.vmId);
    }


    @Test
    public void getNewHfsVmAfterRebuild() {
        when(hfsVmService.getVm(eq(hfsNewVmId))).thenReturn(hfsVm);
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("GetVmAfterCreate"), getHfsVmLambdaCaptor.capture(), eq(Vm.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, Vm> lambda = getHfsVmLambdaCaptor.getValue();
        Vm newHfsVm = lambda.apply(context);
        Assert.assertEquals(newHfsVm.vmId, hfsNewVmId);
    }

    @Test
    public void addsUserToServer() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq(AddUser.class), addUserToServerArgumentCaptor.capture());
        AddUser.Request request = addUserToServerArgumentCaptor.getValue();
        Assert.assertEquals(username, request.username);
    }

    @Test
    public void updatesUsernameForVm() {
        command.execute(context, request);

        verify(spyVmUserService, atLeastOnce()).listUsers(any(UUID.class), eq(VmUserType.CUSTOMER));
        verify(spyVmUserService, atLeastOnce()).deleteUser(any(String.class), any(UUID.class));
        verify(spyVmUserService, atLeastOnce()).createUser(any(String.class), any(UUID.class));
    }

    @Test
    public void setsRootUserPasswordForVm() {
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SetRootUserPassword"), eq(SetPassword.class), setPasswordArgumentCaptor.capture());

        SetPassword.Request request = setPasswordArgumentCaptor.getValue();
        Assert.assertArrayEquals(password.getBytes(), request.encryptedPassword);
        Assert.assertEquals(hfsNewVmId, request.hfsVmId);
        assertThat(
                Arrays.asList("root"),
                containsInAnyOrder(request.usernames.toArray())
        );
    }

    @Test
    public void doesNotSetRootUserPasswordForNonLinuxBasedVm() {
        request.rebuildVmInfo.image = setupPleskImage();
        command.execute(context, request);

        verify(context, times(0))
                .execute(eq("SetRootUserPassword"), eq(SetPassword.class), any());
    }

    @Test
    public void enablesAdminAccessForSelfManagedVm() {
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), toggleAdminArgumentCaptor.capture());

        ToggleAdmin.Request request = toggleAdminArgumentCaptor.getValue();
        Assert.assertEquals(username, request.username);
        Assert.assertEquals(hfsNewVmId, request.vmId);
        Assert.assertTrue(request.enabled);
    }

    @Test
    public void disablesAdminAccessForFullyManagedVm() {
        request.rebuildVmInfo.image = setupcPanelImage();
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), toggleAdminArgumentCaptor.capture());

        ToggleAdmin.Request request = toggleAdminArgumentCaptor.getValue();
        Assert.assertEquals(username, request.username);
        Assert.assertEquals(hfsNewVmId, request.vmId);
        Assert.assertFalse(request.enabled);
    }

    @Test
    public void configuresMailRelayWithNullControlPanel() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(ConfigureMailRelay.class), configMTAArgumentCaptor.capture());
        ConfigureMailRelay.ConfigureMailRelayRequest mtaReq = configMTAArgumentCaptor.getValue();

        Assert.assertNull(mtaReq.controlPanel);
    }

    @Test
    public void configuresMailRelayWithcPanel() {
        request.rebuildVmInfo.image.controlPanel= Image.ControlPanel.CPANEL;
        command.execute(context, request);
        verify(context, times(1)).execute(eq(ConfigureMailRelay.class), configMTAArgumentCaptor.capture());
        ConfigureMailRelay.ConfigureMailRelayRequest mtaReq = configMTAArgumentCaptor.getValue();

        Assert.assertEquals(Image.ControlPanel.CPANEL.toString().toLowerCase(), mtaReq.controlPanel);
    }

    @Test
    public void configuresControlPanelForcPanelImage() {
        request.rebuildVmInfo.image = setupcPanelImage();
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq(ConfigureCpanel.class), configureCpanelRequestArgumentCaptor.capture());

        ConfigureCpanel.ConfigureCpanelRequest configureCpanelRequest = configureCpanelRequestArgumentCaptor.getValue();
        Assert.assertEquals(vps4NewVm.hfsVmId, configureCpanelRequest.vmId);
    }

    @Test
    public void configuresControlPanelForPleskImage() {
        request.rebuildVmInfo.image = setupPleskImage();
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq(ConfigurePlesk.class), configurePleskRequestArgumentCaptor.capture());

        ConfigurePlesk.ConfigurePleskRequest configurePleskRequest = configurePleskRequestArgumentCaptor.getValue();
        Assert.assertEquals(vps4NewVm.hfsVmId, configurePleskRequest.vmId);
    }

    @Test
    public void updatesVirtualMachineDetails() {
        command.execute(context, request);
        Assert.assertEquals(request.rebuildVmInfo.image.imageId,
                vps4VmService.getVirtualMachine(request.rebuildVmInfo.vmId).image.imageId);

        Assert.assertEquals(request.rebuildVmInfo.serverName,
                vps4VmService.getVirtualMachine(request.rebuildVmInfo.vmId).name);
    }

    @Test
    public void doesInvokeSetSetupEcommCommonName() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("SetCommonName"), any(Function.class), eq(Void.class));
    }

    @Test
    public void doesInvokeDeleteSupportUsers() {
        command.execute(context, request);

        verify(spyVmUserService, atLeastOnce()).listUsers(any(UUID.class), eq(VmUserType.SUPPORT));
        verify(spyVmUserService, atLeastOnce()).deleteUser(any(String.class), any(UUID.class));
    }
}
