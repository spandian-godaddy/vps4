package com.godaddy.vps4.orchestration.phase2;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

import com.godaddy.vps4.vm.ActionType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.cpanel.RefreshCpanelLicense;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVmFromSnapshot;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestoreVm;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RestoreVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class Vps4RestoreVmTest {
    static Injector injector;
    private Vps4RestoreVm command;
    private CommandContext context;
    private Vps4RestoreVm.Request request;
    private UUID vps4VmId;
    private UUID vps4SnapshotId;
    private long restoreActionId;
    private Project vps4Project;
    private List<IpAddress> ipAddresses;
    private gdg.hfs.vhfs.vm.VmAction hfsAction;
    private long hfsRestoreActionId = 12345;
    private long hfsNewVmId = 4567;
    private Vm hfsVm;
    private VirtualMachine vps4Vm;
    private VirtualMachineService spyVps4VmService;
    static final String username = "jdoe";
    static final String password = "P@$$w0rd1";

    @Inject private Vps4UserService vps4UserService;
    @Inject private ProjectService projectService;
    @Inject private VmService hfsVmService;
    @Inject private VirtualMachineService vps4VmService;
    @Inject private SnapshotService vps4SnapshotService;
    @Inject private NetworkService vps4NetworkService;
    @Inject private ActionService actionService;
    @Inject private VmUserService vmUserService;

    @Captor private ArgumentCaptor<Function<CommandContext, Long>> getHfsVmIdLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, String>> getVmOSDistroLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Vm>> getHfsVmLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Void>> updateHfsVmIdLambdaCaptor;
    @Captor private ArgumentCaptor<BindIp.BindIpRequest> bindIpRequestArgumentCaptor;
    @Captor private ArgumentCaptor<CreateVmFromSnapshot.Request> flavorRequestArgumentCaptor;
    @Captor private ArgumentCaptor<SetPassword.Request> setPasswordArgumentCaptor;
    @Captor private ArgumentCaptor<ToggleAdmin.Request> toggleAdminArgumentCaptor;
    @Captor private ArgumentCaptor<UnbindIp.Request> unbindIpArgumentCaptor;
    @Captor private ArgumentCaptor<RefreshCpanelLicense.Request> refreshLicenseCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new DatabaseModule(),
                new SecurityModule(),
                new VmModule(),
                new SnapshotModule(),
                new Vps4ExternalsModule()
        );
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);

        spyVps4VmService = spy(vps4VmService);
        command = new Vps4RestoreVm(actionService, hfsVmService, spyVps4VmService,
                vps4NetworkService, vps4SnapshotService, vmUserService);
        addTestSqlData();
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
        vps4Vm = SqlTestData.insertVm(vps4VmService, vps4UserService);
        vps4VmId = vps4Vm.vmId;
        vps4SnapshotId = SqlTestData.insertSnapshotWithStatus(
                vps4SnapshotService, vps4Vm.vmId, vps4Project.getProjectId(), SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        restoreActionId = SqlTestData.insertVmAction(actionService, vps4VmId, ActionType.RESTORE_VM);
        ipAddresses = new ArrayList<>();
        ipAddresses.addAll(
                SqlTestData.insertIpAddresses(vps4NetworkService, vps4VmId, 1, IpAddress.IpAddressType.PRIMARY));
        ipAddresses.addAll(
                SqlTestData.insertIpAddresses(vps4NetworkService, vps4VmId, 2, IpAddress.IpAddressType.SECONDARY));
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());

        when(mockContext.execute(eq("GetHfsVmId"), any(Function.class), eq(long.class))).thenReturn(SqlTestData.hfsVmId);
        when(mockContext.execute(eq("GetVirtualMachine"), any(Function.class), eq(VirtualMachine.class))).thenReturn(vps4Vm);
        when(mockContext.execute(startsWith("UnbindIP-"), eq(UnbindIp.class), any())).thenReturn(null);
        when(mockContext.execute(eq("GetNocfoxImageId"), any(Function.class), eq(String.class))).thenReturn(SqlTestData.nfImageId);
        when(mockContext.execute(eq("GetVmOSDistro"), any(Function.class), eq(String.class))).thenReturn(SqlTestData.IMAGE_NAME);

        hfsAction = new gdg.hfs.vhfs.vm.VmAction();
        hfsAction.vmActionId = hfsRestoreActionId;
        hfsAction.vmId = hfsNewVmId;

        when(mockContext.execute(eq("CreateVmFromSnapshot"), eq(CreateVmFromSnapshot.class), any()))
                .thenReturn(hfsAction);

        hfsVm = new Vm();
        hfsVm.vmId = hfsNewVmId;
        when(mockContext.execute(eq("GetVmAfterCreate"), any(Function.class), eq(Vm.class))).thenReturn(hfsVm);

        when(mockContext.execute(eq("UpdateHfsVmId"), any(Function.class), eq(Void.class))).thenReturn(null);
        when(mockContext.execute(startsWith("BindIP-"), eq(BindIp.class), any())).thenReturn(null);
        when(mockContext.execute(eq("DestroyVmHfs"), eq(DestroyVm.class), eq(hfsNewVmId))).thenReturn(null);
        return mockContext;
    }

    private Vps4RestoreVm.Request getCommandRequest() {
        Vps4RestoreVm.Request req = new Vps4RestoreVm.Request();
        req.actionId = restoreActionId;
        req.restoreVmInfo = new RestoreVmInfo();
        req.restoreVmInfo.vmId = vps4VmId;
        req.restoreVmInfo.snapshotId = vps4SnapshotId;
        req.restoreVmInfo.hostname = "foobar";
        req.restoreVmInfo.username = username;
        req.restoreVmInfo.encryptedPassword = password.getBytes();
        req.restoreVmInfo.zone = "zone-1";
        req.restoreVmInfo.rawFlavor = "rawflavor";
        req.restoreVmInfo.sgid = vps4Project.getVhfsSgid();
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
    public void unbindsExistingIpAddresses() {
        command.execute(context, request);
        for (IpAddress ipAddress: ipAddresses) {
            verify(context, times(1))
                .execute(
                    eq(String.format("UnbindIP-%d", ipAddress.ipAddressId)),
                    eq(UnbindIp.class),
                    unbindIpArgumentCaptor.capture()
                );
            UnbindIp.Request unbindIpRequest = unbindIpArgumentCaptor.getValue();
            Assert.assertTrue(unbindIpRequest.forceIfVmInaccessible);
            Assert.assertEquals(ipAddress.ipAddressId, (long) unbindIpRequest.addressId);
        }
    }

    @Test
    public void getsVmOSDistro() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("GetVmOSDistro"), getVmOSDistroLambdaCaptor.capture(), eq(String.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, String> lambda = getVmOSDistroLambdaCaptor.getValue();
        String osDistro = lambda.apply(context);
        Assert.assertEquals("centos-7", osDistro); // This is because the image used is hfs-centos-7
    }

    @Test
    public void createsVmFromSnapshot() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(
                eq("CreateVmFromSnapshot"), eq(CreateVmFromSnapshot.class),
                flavorRequestArgumentCaptor.capture()
            );

        CreateVmFromSnapshot.Request flavorRequest = flavorRequestArgumentCaptor.getValue();
        Assert.assertEquals( "True", flavorRequest.ignore_whitelist);
        Assert.assertEquals(SqlTestData.nfImageId, flavorRequest.image_id);
        Assert.assertEquals(SqlTestData.IMAGE_NAME, flavorRequest.os);
    }

    @Test
    public void getNewHfsVmAfterCreation() {
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
    public void setsRootUserPasswordForLinuxBasedSnapshot() {
        command.execute(context, request);

        verify(spyVps4VmService, times(1)).isLinux(eq(vps4VmId));
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
    public void doesNotSetRootUserPasswordForNonLinuxBasedSnapshot() {
        doReturn(false).when(spyVps4VmService).isLinux(vps4VmId);
        command.execute(context, request);

        verify(spyVps4VmService, times(1)).isLinux(eq(vps4VmId));
        verify(context, times(0))
                .execute(eq("SetRootUserPassword"), eq(SetPassword.class), any());
    }

    @Test
    public void enablesAdminAccessForSelfManagedVm() {
        command.execute(context, request);

        verify(spyVps4VmService, times(1)).hasControlPanel(eq(vps4VmId));
        verify(context, times(1))
                .execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), toggleAdminArgumentCaptor.capture());

        ToggleAdmin.Request request = toggleAdminArgumentCaptor.getValue();
        Assert.assertEquals(username, request.username);
        Assert.assertEquals(hfsNewVmId, request.vmId);
        Assert.assertTrue(request.enabled);
    }

    @Test
    public void disablesAdminAccessForFullyManagedVm() {
        doReturn(true).when(spyVps4VmService).hasControlPanel(vps4VmId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("ConfigureAdminAccess"), eq(ToggleAdmin.class), toggleAdminArgumentCaptor.capture());

        ToggleAdmin.Request request = toggleAdminArgumentCaptor.getValue();
        Assert.assertEquals(username, request.username);
        Assert.assertEquals(hfsNewVmId, request.vmId);
        Assert.assertFalse(request.enabled);
    }

    @Test
    public void updatesHfsVmIdWithTheNewId() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("UpdateHfsVmId"), updateHfsVmIdLambdaCaptor.capture(), eq(Void.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, Void> lambda = updateHfsVmIdLambdaCaptor.getValue();
        lambda.apply(context);
        Assert.assertEquals(vps4VmService.getVirtualMachine(vps4VmId).hfsVmId, hfsNewVmId);
    }

    @Test
    public void bindsIpAddressesToNewHfsVm() {
        command.execute(context, request);
        for (IpAddress ipAddress: ipAddresses) {
            verify(context, times(1))
                .execute(
                    eq(String.format("BindIP-%d", ipAddress.ipAddressId)),
                    eq(BindIp.class),
                    bindIpRequestArgumentCaptor.capture()
                );

            // verify parameter passed into the BindIp command is the right pair of hfsVmId and ipAddressId
            BindIp.BindIpRequest bindIpRequest = bindIpRequestArgumentCaptor.getValue();
            Assert.assertEquals(bindIpRequest.vmId, hfsNewVmId);
            Assert.assertEquals(bindIpRequest.addressId, ipAddress.ipAddressId);
        }
    }

    @Test
    public void deletesOldHfsVm() {
        command.execute(context, request);

        // SqlTestData.hfsVmId is the ID of the old hfs vm
        verify(context, times(1))
            .execute(eq("DestroyVmHfs"), eq(DestroyVm.class), eq(SqlTestData.hfsVmId));
    }

    @Test
    public void refreshesCpanelLicense() {
        vps4Vm.image = new Image();
        vps4Vm.image.controlPanel = Image.ControlPanel.CPANEL;
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("RefreshCPanelLicense"), eq(RefreshCpanelLicense.class), refreshLicenseCaptor.capture());

        RefreshCpanelLicense.Request refreshRequest = refreshLicenseCaptor.getValue();
        Assert.assertEquals(vps4Vm.hfsVmId, refreshRequest.hfsVmId);
    }
}
