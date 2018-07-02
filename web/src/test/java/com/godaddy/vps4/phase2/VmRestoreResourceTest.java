package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.vm.Vps4RestoreVm;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserType;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.vm.VmRestoreResource;
import com.godaddy.vps4.web.vm.VmRestoreResource.RestoreVmRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class VmRestoreResourceTest {
    private static GDUser us;
    private static GDUser them;
    private static GDUser employee;
    private static GDUser adminWithShopperHeader;
    private static GDUser admin;
    private static GDUser e2sUser;

    private static String goodPassword = "QsxEfv@12345";
    private static String badPassword = "password";

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject SnapshotService vps4SnapshotService;
    @Inject ActionService actionService;


    private GDUser user;
    private VirtualMachine ourVm;
    private VirtualMachine theirVm;
    private Snapshot ourSnapshot;
    private Snapshot theirSnapshot;
    private Vps4User ourVps4User;
    private Vps4User theirVps4User;
    private final Injector injector;

    @Captor private ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor;

    {
        injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {
                @Override
                public void configure() {
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

        injector.injectMembers(this);
    }

    @BeforeClass
    public static void setupUsers() {
        us = GDUserMock.createShopper(GDUserMock.DEFAULT_SHOPPER);
        them = GDUserMock.createShopper("shopperX");
        employee = GDUserMock.createEmployee();
        adminWithShopperHeader = GDUserMock.createAdmin(GDUserMock.DEFAULT_SHOPPER);
        admin = GDUserMock.createAdmin();
        e2sUser = GDUserMock.createEmployee2Shopper();
    }

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);

        ourVps4User = userService.getOrCreateUserForShopper(us.getShopperId(), "1");
        theirVps4User = userService.getOrCreateUserForShopper(them.getShopperId(), "1");
        ourVm = createVm(ourVps4User.getId());
        theirVm = createVm(theirVps4User.getId());
        ourSnapshot = createSnapshot(ourVm.vmId, ourVm.projectId);
        theirSnapshot = createSnapshot(theirVm.vmId, theirVm.projectId);
        createVmUser(ourVm.vmId);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VirtualMachine createVm(long vps4UserId) {
        return SqlTestData.insertTestVm(UUID.randomUUID(), vps4UserId, dataSource);
    }

    private Snapshot createSnapshot(UUID vmId, long projectId) {
        return SqlTestData.insertSnapshotWithStatus(
            vps4SnapshotService, vmId, projectId, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
    }

    private void createVmUser(UUID vmId) {
        VmUser testUser = new VmUser("testUser", vmId, true, VmUserType.CUSTOMER);
        SqlTestData.insertTestUser(testUser, dataSource);
    }

    private VmRestoreResource getVmRestoreResource() {
        return injector.getInstance(VmRestoreResource.class);
    }

    private RestoreVmRequest getRequestPayload(UUID snapshotId, String password) {
        RestoreVmRequest req = new RestoreVmRequest();
        req.backupId = snapshotId;
        req.password = password;
        return req;
    }

    // === Shopper Tests ===

    private void verifySuccessfulVmRestorationByAdmin() {
        VmAction vmAction = getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(null, null));

        Assert.assertEquals(vmAction.type, ActionType.RESTORE_VM);
        Assert.assertEquals(vmAction.virtualMachineId, ourVm.vmId);
        verifyCommandRequestParams(ourVm, ourSnapshot.id, goodPassword);
    }

    private void verifySuccessfulVmRestoration() {
        VmAction vmAction = getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));

        Assert.assertEquals(vmAction.type, ActionType.RESTORE_VM);
        Assert.assertEquals(vmAction.virtualMachineId, ourVm.vmId);
        verifyCommandRequestParams(ourVm, ourSnapshot.id, goodPassword);
    }

    private void verifyCommandRequestParams(VirtualMachine vm, UUID snapshotId, String password) {
        CommandService commandService = injector.getInstance(CommandService.class);
        verify(commandService, times(1))
                .executeCommand(commandGroupSpecArgumentCaptor.capture());

        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Vps4RestoreVm.Request commandRequest = (Vps4RestoreVm.Request) commandSpec.request;

        Assert.assertEquals("Vps4RestoreVm", commandSpec.command);
        Assert.assertNotNull(commandRequest.restoreVmInfo.encryptedPassword);
        Assert.assertEquals(commandRequest.restoreVmInfo.vmId, vm.vmId);
        Assert.assertEquals(commandRequest.restoreVmInfo.hostname, vm.hostname);
        Assert.assertEquals(commandRequest.restoreVmInfo.snapshotId, snapshotId);
    }

    @Test
    public void verifyAdminRestoreVm() {
        user = admin;
        verifySuccessfulVmRestorationByAdmin();
    }

    @Test(expected = AuthorizationException.class)
    public void failRestoreVmForNonAdminUsers() {
        user = them;
        verifySuccessfulVmRestoration();
    }

    @Test
    public void weCanRestoreOurVm() {
        user = us;
        verifySuccessfulVmRestoration();
    }

    @Test(expected = NotFoundException.class)
    public void weCantRestoreANonExistentVm() {
        user = us;
        getVmRestoreResource().restore(UUID.randomUUID(), getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreUsingANonExistentSnapshot() {
        user = us;
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(UUID.randomUUID(), goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreUsingTheirSnapshot() {
        user = us;
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(theirSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreUsingBadPassword() {
        user = us;
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, badPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreUsingANonLiveSnapshot() {
        user = us;
        vps4SnapshotService.markSnapshotInProgress(ourSnapshot.id);
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreWhenAVmIsCurrentlyRestarting() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.RESTART_VM, new JSONObject().toJSONString(), "tester");
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreWhenAVmIsCurrentlyStopping() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.STOP_VM, new JSONObject().toJSONString(), "tester");
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreWhenAVmIsCurrentlyStarting() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.START_VM, new JSONObject().toJSONString(), "tester");
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreWhenAVmIsAlreadyRestoring() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.RESTORE_VM, new JSONObject().toJSONString(), "tester");
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantRestoreWhenAVmIsStillBeingCreated() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.CREATE_VM, new JSONObject().toJSONString(), "tester");
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test(expected = AuthorizationException.class)
    public void theyCantRestoreOurVM() {
        user = them;
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(theirSnapshot.id, goodPassword));
    }

    @Test
    public void weCantRestoreIfSuspended() {
        user = us;
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            verifySuccessfulVmRestoration();
            Assert.fail("Exception not thrown");
        } catch(Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    // === Employee Tests ===

    @Test
    public void anEmployeeCanRestoreOurVM() {
        user = employee;
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }

    @Test
    public void anEmployeeWithDelegationCanRestoreOurVm() {
        user = e2sUser;
        verifySuccessfulVmRestoration();
    }

    // === Admin Tests ===

    @Test
    public void anAdminWithShopperHeaderSetCanRestoreOurVm() {
        user = adminWithShopperHeader;
        verifySuccessfulVmRestorationByAdmin();
    }

    @Test
    public void anAdminWithoutShopperHeaderCanRestoreOurVM() {
        user = admin;
        getVmRestoreResource().restore(ourVm.vmId, getRequestPayload(ourSnapshot.id, goodPassword));
    }
}
