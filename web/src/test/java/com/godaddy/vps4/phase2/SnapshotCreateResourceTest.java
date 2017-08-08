package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.*;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import org.junit.*;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.UUID;

public class SnapshotCreateResourceTest {
    private static GDUser us;
    private static GDUser them;
    private static GDUser employee;
    private static GDUser adminWithShopperHeader;
    private static GDUser admin;
    private static GDUser e2sUser;

    @BeforeClass
    public static void setupUsers() {
        us = GDUserMock.createShopper();
        them = GDUserMock.createShopper("shopperX");
        employee = GDUserMock.createEmployee();
        adminWithShopperHeader = GDUserMock.createAdmin(GDUserMock.DEFAULT_SHOPPER);
        admin = GDUserMock.createAdmin();
        e2sUser = GDUserMock.createEmployee2Shopper();
    }

    @Inject
    Vps4UserService userService;

    @Inject
    DataSource dataSource;

    @Inject
    SnapshotService vps4SnapshotService;

    private GDUser user;
    private UUID ourVmId;
    private Vps4User ourVps4User;

    private final Injector injector;

    {
        injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {
                @Override
                public void configure() {
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

        injector.injectMembers(this);
        ourVps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
    }


    @Before
    public void setupTest() {
        ourVmId = createOurVm();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private UUID createOurVm() {
        VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), ourVps4User.getId(), dataSource);
        return testVm.vmId;
    }

    private SnapshotResource getSnapshotResource() {
        return injector.getInstance(SnapshotResource.class);
    }

    private SnapshotResource.SnapshotRequest getRequestPayload(UUID vmId, String name) {
        SnapshotResource.SnapshotRequest snapshotRequest = new SnapshotResource.SnapshotRequest();
        snapshotRequest.name = name;
        snapshotRequest.vmId = vmId;
        return snapshotRequest;
    }

    // === Shopper Tests ===

    private void verifySuccessfulSnapshotCreation() {
        SnapshotAction snapshotAction = getSnapshotResource()
                .createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));

        Assert.assertEquals(snapshotAction.vps4UserId, ourVps4User.getId());
        verifyActionAssociatedWithSnapshot(snapshotAction);
        verifyCommandExecution();
    }

    private void verifyCommandExecution() {
        CommandService commandService = injector.getInstance(CommandService.class);
        verify(commandService, times(1)).executeCommand(any(CommandGroupSpec.class));
    }

    private void verifyActionAssociatedWithSnapshot(SnapshotAction snapshotAction) {
        List<Snapshot> snapshots = vps4SnapshotService.getSnapshotsForVm(ourVmId);
        Assert.assertEquals(1, snapshots.size());
        Assert.assertEquals(snapshots.get(0).id, snapshotAction.snapshotId);
    }

    @Test
    public void weCanSnapshotOurVm() {
        user = us;
        verifySuccessfulSnapshotCreation();
    }

    @Test(expected = NotFoundException.class)
    public void weCantSnapshotANonExistentVm() {
        user = us;
        getSnapshotResource().createSnapshot(getRequestPayload(UUID.randomUUID(), SqlTestData.TEST_SNAPSHOT_NAME));
    }

    @Test(expected = Vps4Exception.class)
    public void weCantSnapshotWhenOverQuota() {
        // if all the snapshots that have filled up the slots aren't yet LIVE
        user = us;
        getSnapshotResource().createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
        // the snapshot created on the previous line is in NEW status, so the next request (next line) is rejected
        getSnapshotResource().createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
    }

    @Test
    public void weCanSnapshotWhenAllTheSlotsAreFilledOnlyByLiveSnapshots() {
        user = us;
        SnapshotAction action1 = getSnapshotResource()
                .createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
        vps4SnapshotService.markSnapshotLive(action1.snapshotId);
        // Now the previous snapshot shot is live, hence the request on the next line should be accepted
        SnapshotAction action2 = getSnapshotResource()
                .createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));

        Assert.assertEquals(SnapshotStatus.LIVE, vps4SnapshotService.getSnapshot(action1.snapshotId).status);
        Assert.assertEquals(SnapshotStatus.NEW, vps4SnapshotService.getSnapshot(action2.snapshotId).status);
    }

    @Test(expected = Vps4Exception.class)
    public void weCantSnapshotWithABadName() {
        user = us;
        getSnapshotResource().createSnapshot(getRequestPayload(ourVmId, "Thisnameistoolongtobevalid"));
    }

    @Test(expected = AuthorizationException.class)
    public void theyCantSnapshotOurVM() {
        user = them;
        getSnapshotResource().createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
    }

    @Test
    public void weCantSnapshotIfSuspended() {
        user = us;
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            verifySuccessfulSnapshotCreation();
            Assert.fail("Exception not thrown");
        } catch(Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    // === Employee Tests ===

    @Test(expected = Vps4NoShopperException.class)
    public void anEmployeeCantSnapshotOurVM() {
        user = employee;
        getSnapshotResource().createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
    }

    @Test
    public void anEmployeeWithDelegationCanSnapshotOurVm() {
        user = e2sUser;
        verifySuccessfulSnapshotCreation();
    }

    // === Admin Tests ===

    @Test
    public void anAdminWithShopperHeaderSetCanSnapshotOurVm() {
        user = adminWithShopperHeader;
        verifySuccessfulSnapshotCreation();
    }

    @Test(expected = Vps4NoShopperException.class)
    public void anAdminWithoutShopperHeaderCantSnapshotOurVM() {
        user = admin;
        getSnapshotResource().createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
    }
}
