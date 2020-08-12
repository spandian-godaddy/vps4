package com.godaddy.vps4.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;

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

    @Inject
    VmService vmService;

    private GDUser user;
    private UUID ourVmId, ourVmId2;
    private Vps4User ourVps4User;
    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);

    private final Injector injector;

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
                    bind(SchedulerWebService.class).toInstance(schedulerWebService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

        injector.injectMembers(this);
        ourVps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
    }


    @Before
    public void setupTest() {
        ourVmId = createOurVm();
        ourVmId2 = createOurVm();
        SchedulerJobDetail jobDetail = new SchedulerJobDetail(UUID.randomUUID(), null, null, false);
        when(schedulerWebService.getJob(eq("vps4"), eq("backups"),any(UUID.class) )).thenReturn(jobDetail);

        VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();
        vmExtendedInfoMock.provider = "nocfox";
        vmExtendedInfoMock.resource = "openstack";
        Extended extendedMock = new Extended();
        extendedMock.hypervisorHostname = "n3plztncldhv001-02.prod.ams3.gdg";
        vmExtendedInfoMock.extended = extendedMock;
        when(vmService.getVmExtendedInfo(anyLong())).thenReturn(vmExtendedInfoMock);
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
        snapshotRequest.snapshotType = SnapshotType.ON_DEMAND;
        return snapshotRequest;
    }

    // === Shopper Tests ===

    private void verifySuccessfulSnapshotCreation() {
        SnapshotAction snapshotAction = getSnapshotResource()
                .createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
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

    @Test
    public void weCanSnapshotWhenSnapshotSetToErrorRescheduled() {
        // created because there was a bug when error_rescheduled was introduced that rejected snapshot
        // requests because "a snapshot was in progress" when a snapshot status was error_rescheduled.
        user = us;
        SnapshotAction action1 = getSnapshotResource()
                .createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));
        vps4SnapshotService.markSnapshotErrorRescheduled(action1.snapshotId);

        SnapshotAction action2 = getSnapshotResource()
                .createSnapshot(getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME));

        Assert.assertEquals(SnapshotStatus.ERROR_RESCHEDULED, vps4SnapshotService.getSnapshot(action1.snapshotId).status);
        Assert.assertEquals(SnapshotStatus.NEW, vps4SnapshotService.getSnapshot(action2.snapshotId).status);

    }

    @Test
    public void weCanCreateAnAutomaticSnapshot() {
        user = us;
        SnapshotRequest request = getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME);
        request.snapshotType = SnapshotType.AUTOMATIC;
        SnapshotAction action1 = getSnapshotResource().createSnapshot(request);
        vps4SnapshotService.markSnapshotLive(action1.snapshotId);
        Assert.assertEquals(SnapshotType.AUTOMATIC, vps4SnapshotService.getSnapshot(action1.snapshotId).snapshotType);

    }

    @Test(expected = Vps4Exception.class)
    public void weCannotCreateAutomaticSnapshotIfPaused() {
        SchedulerJobDetail jobDetail = new SchedulerJobDetail(UUID.randomUUID(), null, null, true);
        when(schedulerWebService.getJob(eq("vps4"), eq("backups"),any(UUID.class) )).thenReturn(jobDetail);
        user = us;
        SnapshotRequest request = getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME);
        request.snapshotType = SnapshotType.AUTOMATIC;
        getSnapshotResource().createSnapshot(request);

    }

    @Test
    public void weCanCreateManualSnapshotIfPaused() {
        SchedulerJobDetail jobDetail = new SchedulerJobDetail(UUID.randomUUID(), null, null, true);
        when(schedulerWebService.getJob(eq("vps4"), eq("backups"),any(UUID.class) )).thenReturn(jobDetail);
        user = us;
        SnapshotRequest request = getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME);
        request.snapshotType = SnapshotType.ON_DEMAND;
        SnapshotAction action1 = getSnapshotResource().createSnapshot(request);
        vps4SnapshotService.markSnapshotLive(action1.snapshotId);
        Assert.assertEquals(SnapshotType.ON_DEMAND, vps4SnapshotService.getSnapshot(action1.snapshotId).snapshotType);
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

    @Test
    public void testThrowsVps4ExceptionIfReachHvConcurrencyLimit() {
        user = us;
        SnapshotRequest request = getRequestPayload(ourVmId, SqlTestData.TEST_SNAPSHOT_NAME);
        request.snapshotType = SnapshotType.AUTOMATIC;
        SnapshotAction action1 = getSnapshotResource().createSnapshot(request);
        vps4SnapshotService.markSnapshotLive(action1.snapshotId);
        try {
            getSnapshotResource().createSnapshot(request);
            Assert.fail("Exception not thrown");
        } catch(Vps4Exception e) {
            Assert.assertEquals("SNAPSHOT_HV_LIMIT_REACHED", e.getId());
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
