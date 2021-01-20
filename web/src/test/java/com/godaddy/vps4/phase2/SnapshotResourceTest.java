package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRenameRequest;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class SnapshotResourceTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject SnapshotService snapshotService;

    private GDUser user;
    private VirtualMachine testVm;
    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);

    private Injector injector = Guice.createInjector(
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


    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private SnapshotResource getSnapshotResource() {
        return injector.getInstance(SnapshotResource.class);
    }

    private void createTestVm() {
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
    }

    private Snapshot createTestSnapshot() {
        createTestVm();
        return SqlTestData.insertSnapshot(snapshotService, testVm.vmId, testVm.projectId, SnapshotType.ON_DEMAND);
    }

    // === getSnapshotList Tests ===
    private void testGetSnapshotList() {
        Snapshot snapshot = createTestSnapshot();

        List<Snapshot> snapshots = getSnapshotResource().getSnapshotsForUser();
        Assert.assertEquals(1, snapshots.size());
        Assert.assertEquals(snapshots.get(0).vmId, snapshot.vmId);
    }

    @Test
    public void testShopperGetSnapshotList() {
        user = GDUserMock.createShopper();
        testGetSnapshotList();
    }

    @Test(expected = Vps4NoShopperException.class)
    public void testAdminFailsGetSnapshotList() {
        user = GDUserMock.createAdmin();
        testGetSnapshotList();
    }

    @Test
    public void testUnauthorizedShopperGetSnapshotList() {
        createTestSnapshot();

        user = GDUserMock.createShopper("shopperX");
        List<Snapshot> snapshots = getSnapshotResource().getSnapshotsForUser();
        Assert.assertEquals(0, snapshots.size());
    }

    // === getSnapshot Tests ===
    private void testGetSnapshot() {
        Snapshot snapshot = createTestSnapshot();
        UUID expectedVmId = snapshot.vmId;

        snapshot = getSnapshotResource().getSnapshot(snapshot.id);
        Assert.assertEquals(expectedVmId, snapshot.vmId);
    }

    @Test
    public void testGetSnapshotInternalFieldsHidden() throws JsonProcessingException {
        Snapshot snapshot = createTestSnapshot();

        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writerWithView(Views.Public.class).writeValueAsString(snapshot);
        Assert.assertFalse(result.contains("hfsImageId"));
    }

    @Test
    public void testShopperGetSnapshot() {
        user = GDUserMock.createShopper();
        testGetSnapshot();
    }

    @Test(expected = AuthorizationException.class)
    public void testUnauthorizedShopperGetSnapshot() {
        user = GDUserMock.createShopper("shopperX");
        testGetSnapshot();
    }

    @Test(expected = NotFoundException.class)
    public void testNoVmGetSnapshot() {
        user = GDUserMock.createShopper();
        UUID noSuchSnapshotId = UUID.randomUUID();

        getSnapshotResource().getSnapshot(noSuchSnapshotId);
    }

    @Test
    public void testShopperGetDeletedSnapshot() {
        Snapshot snapshot = createTestSnapshot();
        SqlTestData.invalidateSnapshot(snapshotService, snapshot.id);

        user = GDUserMock.createShopper();
        try {
            getSnapshotResource().getSnapshot(snapshot.id);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("SNAPSHOT_DELETED", e.getId());
        }
    }

    @Test
    public void testAdminGetDeletedSnapshot() {
        Snapshot snapshot = createTestSnapshot();
        SqlTestData.invalidateSnapshot(snapshotService, snapshot.id);
        UUID expectedVmId = snapshot.vmId;

        user = GDUserMock.createAdmin();
        snapshot = getSnapshotResource().getSnapshot(snapshot.id);
        Assert.assertEquals(expectedVmId, snapshot.vmId);
    }

    @Test
    public void testEmployeeGetSnapshot() {
        user = GDUserMock.createEmployee();
        testGetSnapshot();
    }

    @Test
    public void testAdminGetSnapshot() {
        user = GDUserMock.createAdmin();
        testGetSnapshot();
    }

    @Test
    public void testE2SGetSnapshot() {
        user = GDUserMock.createEmployee2Shopper();
        testGetSnapshot();
    }

    @Test
    public void testShopperGetSnapshotFailsIfSuspended() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        user = GDUserMock.createShopper();
        try {
            testGetSnapshot();
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    // === getSnapshotWithDetails Tests ===
    @Test
    public void testEmployeeGetSnapshotWithDetails() {
        Snapshot snapshot = createTestSnapshot();
        UUID expectedVmId = snapshot.vmId;
        long expectedHfsSnapshotId = snapshot.hfsSnapshotId;

        user = GDUserMock.createEmployee();
        snapshot = getSnapshotResource().getSnapshotWithDetails(snapshot.id);
        Assert.assertEquals(expectedVmId, snapshot.vmId);
        Assert.assertEquals(expectedHfsSnapshotId, snapshot.hfsSnapshotId);
    }

    @Test
    public void testShopperGetSnapshotWithDetails() {
        GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN};
        Assert.assertArrayEquals(expectedRoles, SnapshotResource.class.getAnnotation(RequiresRole.class).roles());
    }

    @Test
    public void testGetSnapshotWithDetailsInternalFieldsShown() throws JsonProcessingException {
        Snapshot snapshot = createTestSnapshot();

        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writerWithView(Views.Internal.class).writeValueAsString(snapshot);
        Assert.assertTrue(result.contains("hfsImageId"));
    }

    // === destroySnapshot Tests ===
    private void testDestroySnapshot() {
        Snapshot snapshot = createTestSnapshot();

        String hvsHostname = "test_" + snapshot.vmId;
        Assert.assertEquals(snapshot.vmId, snapshotService.getVmIdWithInProgressSnapshotOnHv(hvsHostname));

        SnapshotAction snapshotAction = getSnapshotResource().destroySnapshot(snapshot.id);
        Assert.assertNotNull(snapshotAction.commandId);

        Assert.assertNull(snapshotService.getVmIdWithInProgressSnapshotOnHv(hvsHostname));
    }

    @Test
    public void testShopperDestroySnapshot() {
        testDestroySnapshot();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperDestroySnapshot() {
        user = GDUserMock.createShopper("shopperX");
        testDestroySnapshot();
    }

    @Test
    public void testAdminDestroySnapshot() {
        user = GDUserMock.createAdmin();
        testDestroySnapshot();
    }

    @Test
    public void testE2SDestroySnapshot() {
        user = GDUserMock.createEmployee2Shopper();
        testDestroySnapshot();
    }

    // === renameSnapshot Tests ===
    private void testRenameSnapshot() {
        testRenameSnapshot("snappy");
    }

    public void testRenameSnapshot(String name) {
        Snapshot snapshot = createTestSnapshot();

        SnapshotRenameRequest req= new SnapshotRenameRequest();
        req.name = name;
        SnapshotAction action = getSnapshotResource().renameSnapshot(snapshot.id, req);
        Assert.assertEquals(ActionStatus.COMPLETE, action.status);

        snapshot = getSnapshotResource().getSnapshot(snapshot.id);
        Assert.assertEquals(req.name,  snapshot.name);
    }

    @Test
    public void testShopperRenameSnapshot() {
        testRenameSnapshot();
    }

    @Test
    public void testRenameSnapshotWithInvalidName() {
        List<String> invalidNames = Arrays.asList(
                "snap",         // too short
                "f*ing-snap",   // no special chars
                "my snap",      // no spaces
                "my_long_snap-name"  // too long
                );
        for (String name: invalidNames) {
            try {
                testRenameSnapshot(name);
                Assert.fail("Exception not thrown");
            } catch (Vps4Exception e) {
                Assert.assertEquals("INVALID_SNAPSHOT_NAME", e.getId());
            }
        }
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperRenameSnapshot() {
        user = GDUserMock.createShopper("shopperX");
        testRenameSnapshot();
    }

    @Test
    public void testSuspendedShopperRenameSnapshot() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            testRenameSnapshot();
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    @Test
    public void testAdminRenameSnapshot() {
        user = GDUserMock.createAdmin();
        testRenameSnapshot();
    }

    @Test
    public void testE2SRenameSnapshot() {
        user = GDUserMock.createEmployee2Shopper();
        testRenameSnapshot();
    }

    @Test
    public void testCreateSnapshotForDestroyedVm() {
        createTestVm();
        SqlTestData.markVmDeleted(testVm.vmId, dataSource);
        SnapshotRequest request = new SnapshotRequest();
        request.vmId = testVm.vmId;

        try {
            getSnapshotResource().createSnapshot(request);
            Assert.fail("Expected Vps4Exception was not thrown");
        } catch (Vps4Exception ex) {
            Assert.assertEquals("VM_DELETED", ex.getId());
        }
    }

    @Test
    public void testSnapshotFailsWhenNydusIsDown() {
        createTestVm();
        SnapshotRequest request = new SnapshotRequest();
        request.vmId = testVm.vmId;
        Phase2ExternalsModule.mockNydusDown();
        try {
            getSnapshotResource().createSnapshot(request);
            Assert.fail("RuntimeException should have been thrown");
        } catch (Vps4Exception ex) {
            Assert.assertEquals("AGENT_DOWN", ex.getId());}
    }

}
