package com.godaddy.vps4.phase2;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotWithDetails;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class SnapshotResourceTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;

    private Injector injector = Guice.createInjector(
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

    private VmSnapshotResource getVmSnapshotResource() {
        return injector.getInstance(VmSnapshotResource.class);
    }

    private SnapshotWithDetails createTestSnapshot() {
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
        SnapshotWithDetails testSnapshot = new SnapshotWithDetails(
                UUID.randomUUID(),
                "test",
                testVm.projectId,
                (int) (Math.random() * 100000),
                testVm.vmId,
                "test-snapshot",
                SnapshotStatus.LIVE,
                Instant.now(),
                null
        );
        SqlTestData.insertTestSnapshot(testSnapshot, dataSource);
        return testSnapshot;
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
    public void testAdminFailsGetSnapshotList() throws InterruptedException {
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

    @Test(expected = NotFoundException.class)
    public void testNoLongerValidGetSnapshot() {
        Snapshot snapshot = createTestSnapshot();
        SqlTestData.invalidateTestSnapshot(snapshot.id, dataSource);

        user = GDUserMock.createShopper();
        getSnapshotResource().getSnapshot(snapshot.id);
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

    @Ignore("Snapshots need rework, snapshot api should not work if vm suspended")
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
        SnapshotWithDetails snapshot = createTestSnapshot();
        UUID expectedVmId = snapshot.vmId;
        long expectedHfsSnapshotId = snapshot.hfsSnapshotId;

        user = GDUserMock.createEmployee();
        snapshot = getSnapshotResource().getSnapshotWithDetails(snapshot.id);
        Assert.assertEquals(expectedVmId, snapshot.vmId);
        Assert.assertEquals(expectedHfsSnapshotId, snapshot.hfsSnapshotId);
    }

    @Test
    public void testShopperGetSnapshotWithDetails() {
        try {
            Method method = SnapshotResource.class.getMethod("getSnapshotWithDetails", UUID.class);
            Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
        } catch (NoSuchMethodException ex) {
            Assert.fail();
        }
    }

    // === getSnapshotByVmId Tests ===

    private void testGetSnapshotByVmId() {
        Snapshot snapshot = createTestSnapshot();

        List<Snapshot> snapshots = getVmSnapshotResource().getSnapshotsForVM(snapshot.vmId);
        Assert.assertEquals(1, snapshots.size());
        Assert.assertEquals(snapshots.get(0).vmId, snapshot.vmId);
    }

    @Test
    public void testShopperGetSnapshotByVmId() {
        user = GDUserMock.createShopper();
        testGetSnapshotByVmId();
    }

    @Test(expected = AuthorizationException.class)
    public void testUnauthorizedShopperGetSnapshotByVmId() {
        user = GDUserMock.createShopper("shopperX");
        testGetSnapshotByVmId();
    }

    // === destroySnapshot Tests ===
    public void testDestroySnapshot() {
        Snapshot snapshot = createTestSnapshot();

        SnapshotAction snapshotAction = getSnapshotResource().destroySnapshot(snapshot.id);
        Assert.assertNotNull(snapshotAction.commandId);
    }

    @Test
    public void testShopperDestroyVm() {
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

}
