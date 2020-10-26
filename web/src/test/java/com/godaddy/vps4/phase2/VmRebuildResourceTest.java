package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

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

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.vm.rebuild.Vps4RebuildVm;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserType;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmRebuildResource;
import com.godaddy.vps4.web.vm.VmRebuildResource.RebuildVmRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;

public class VmRebuildResourceTest {
    private static GDUser us;
    private static GDUser them;
    private static GDUser employee;
    private static GDUser adminWithShopperHeader;
    private static GDUser admin;
    private static GDUser e2sUser;

    private static String goodPassword = "QsxEfv@12345";
    private static String badPassword = "password";
    private static String imageName = "hfs-centos-7";

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject ActionService actionService;

    private GDUser user;
    private VirtualMachine ourVm;
    private VirtualMachine ourDedicated;
    private Vps4User ourVps4User;
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
        ourVm = createVm(ourVps4User.getId());
        ourDedicated = createDedicatedVm(ourVps4User.getId());
        createVmUser(ourVm.vmId);
        createVmUser(ourDedicated.vmId);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VirtualMachine createVm(long vps4UserId) {
        return SqlTestData.insertTestVm(UUID.randomUUID(), vps4UserId, dataSource);
    }

    private VirtualMachine createDedicatedVm(long vps4UserId) {
        return SqlTestData.insertDedicatedTestVm(UUID.randomUUID(), dataSource);
    }

    private void createVmUser(UUID vmId) {
        VmUser testUser = new VmUser("testUser", vmId, true, VmUserType.CUSTOMER);
        SqlTestData.insertTestUser(testUser, dataSource);
    }

    private VmRebuildResource getVmRebuildResource() {
        return injector.getInstance(VmRebuildResource.class);
    }

    private RebuildVmRequest getRequestPayload(String password, String imageName) {
        RebuildVmRequest req = new RebuildVmRequest();
        req.imageName = imageName;
        req.password = password;
        return req;
    }

    // === Shopper Tests ===

    private void verifySuccessfulVmRebuildByAdmin() {
        VmAction vmAction = getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(null, null));

        Assert.assertEquals(vmAction.type, ActionType.REBUILD_VM);
        Assert.assertEquals(vmAction.virtualMachineId, ourVm.vmId);
        verifyCommandRequestParams(ourVm);
    }

    private void verifySuccessfulVmRebuild() {
        VmAction vmAction = getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));

        Assert.assertEquals(vmAction.type, ActionType.REBUILD_VM);
        Assert.assertEquals(vmAction.virtualMachineId, ourVm.vmId);
        verifyCommandRequestParams(ourVm);
    }

    private void verifySuccessfulDedicatedRebuildByAdmin() {
        VmAction vmAction = getVmRebuildResource().rebuild(ourDedicated.vmId, getRequestPayload(null, null));

        Assert.assertEquals(vmAction.type, ActionType.REBUILD_VM);
        Assert.assertEquals(vmAction.virtualMachineId, ourDedicated.vmId);
    }

    private void verifySuccessfulDedicatedRebuild() {
        VmAction vmAction = getVmRebuildResource().rebuild(ourDedicated.vmId, getRequestPayload(goodPassword, imageName));

        Assert.assertEquals(vmAction.type, ActionType.REBUILD_VM);
        Assert.assertEquals(vmAction.virtualMachineId, ourDedicated.vmId);
    }

    private void verifyCommandRequestParams(VirtualMachine vm) {
        CommandService commandService = injector.getInstance(CommandService.class);
        verify(commandService, times(1))
                .executeCommand(commandGroupSpecArgumentCaptor.capture());

        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Vps4RebuildVm.Request commandRequest = (Vps4RebuildVm.Request) commandSpec.request;

        Assert.assertEquals("Vps4RebuildVm", commandSpec.command);
        Assert.assertNotNull(commandRequest.rebuildVmInfo.encryptedPassword);
        Assert.assertEquals(commandRequest.rebuildVmInfo.vmId, vm.vmId);
        Assert.assertEquals(commandRequest.rebuildVmInfo.hostname, vm.hostname);
    }

    @Test
    public void verifyAdminRebuildDedicated() {
        user = admin;
        verifySuccessfulDedicatedRebuildByAdmin();
    }

    @Test(expected = AuthorizationException.class)
    public void failRebuildDedicatedForNonAdminUsers() {
        user = them;
        verifySuccessfulDedicatedRebuild();
    }

    @Test
    public void weCanRebuildOurDedicated() {
        user = us;
        verifySuccessfulDedicatedRebuild();
    }

    @Test
    public void verifyAdminRebuildVm() {
        user = admin;
        verifySuccessfulVmRebuildByAdmin();
    }

    @Test(expected = AuthorizationException.class)
    public void failRebuildVmForNonAdminUsers() {
        user = them;
        verifySuccessfulVmRebuild();
    }

    @Test
    public void weCanRebuildOurVm() {
        user = us;
        verifySuccessfulVmRebuild();
    }

    @Test(expected = NotFoundException.class)
    public void weCannottRebuildANonExistentVm() {
        user = us;
        getVmRebuildResource().rebuild(UUID.randomUUID(), getRequestPayload(goodPassword, imageName));
    }

    @Test(expected = Vps4Exception.class)
    public void weCannotRebuildUsingBadPassword() {
        user = us;
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(badPassword, imageName));
    }

    @Test
    public void weCanRebuildWhenAVmIsCurrentlyRestarting() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.RESTART_VM, new JSONObject().toJSONString(), "tester");
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test
    public void weCanRebuildWhenAVmIsCurrentlyStopping() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.STOP_VM, new JSONObject().toJSONString(), "tester");
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test
    public void weCanRebuildWhenAVmIsCurrentlyStarting() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.START_VM, new JSONObject().toJSONString(), "tester");
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test(expected = Vps4Exception.class)
    public void weCannotRebuildWhenAVmIsAlreadyRebuilding() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.REBUILD_VM, new JSONObject().toJSONString(), "tester");
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test(expected = Vps4Exception.class)
    public void weCannotRebuildWhenAVmIsBeingRestored() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.RESTORE_VM, new JSONObject().toJSONString(), "tester");
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test(expected = Vps4Exception.class)
    public void weCannotRebuildWhenAVmIsStillBeingCreated() {
        user = us;
        actionService.createAction(
                ourVm.vmId, ActionType.CREATE_VM, new JSONObject().toJSONString(), "tester");
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test(expected = AuthorizationException.class)
    public void theyCannotRebuildOurVM() {
        user = them;
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test
    public void weCannotRebuildIfSuspended() {
        user = us;
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            verifySuccessfulVmRebuild();
            Assert.fail("Exception not thrown");
        } catch(Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    // === Employee Tests ===

    @Test
    public void anEmployeeCanRebuildOurVM() {
        user = employee;
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }

    @Test
    public void anEmployeeWithDelegationCanRebuildOurVm() {
        user = e2sUser;
        verifySuccessfulVmRebuild();
    }

    // === Admin Tests ===

    @Test
    public void anAdminWithShopperHeaderSetCanRebuildOurVm() {
        user = adminWithShopperHeader;
        verifySuccessfulVmRebuildByAdmin();
    }

    @Test
    public void anAdminWithoutShopperHeaderCanRebuildOurVM() {
        user = admin;
        getVmRebuildResource().rebuild(ourVm.vmId, getRequestPayload(goodPassword, imageName));
    }
}
