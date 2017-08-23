package com.godaddy.vps4.phase2;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.snapshot.SnapshotResource;
import com.godaddy.vps4.web.snapshot.SnapshotResource.SnapshotRenameRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SnapshotResourceTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;
    private VirtualMachine testVm;

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

    private Snapshot createTestSnapshot() {
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
        Snapshot testSnapshot = new Snapshot(
                UUID.randomUUID(),
                testVm.projectId,
                testVm.vmId,
                "test-snapshot",
                SnapshotStatus.LIVE,
                Instant.now(),
                null,
                "test-imageid",
                (int) (Math.random() * 100000),
                SnapshotType.ON_DEMAND
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
        try {
            Method method = SnapshotResource.class.getMethod("getSnapshotWithDetails", UUID.class);
            Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
        } catch (NoSuchMethodException ex) {
            Assert.fail();
        }
    }

    @Test
    public void testGetSnapshotWithDetailsInternalFieldsShown() throws JsonProcessingException {
        Snapshot snapshot = createTestSnapshot();

        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writerWithView(Views.Internal.class).writeValueAsString(snapshot);
        Assert.assertTrue(result.contains("hfsImageId"));
    }

    // === destroySnapshot Tests ===
    public void testDestroySnapshot() {
        Snapshot snapshot = createTestSnapshot();

        SnapshotAction snapshotAction = getSnapshotResource().destroySnapshot(snapshot.id);
        Assert.assertNotNull(snapshotAction.commandId);
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
    public void testRenameSnapshot() {
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
                "mySnap",       // no caps
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

}
