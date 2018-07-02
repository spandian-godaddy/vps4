package com.godaddy.vps4.phase2;


import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmSnapshotResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject SnapshotService snapshotService;

    private GDUser user;
    private VirtualMachine testVm;

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

    private Snapshot createTestSnapshot() {
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
        return SqlTestData.insertSnapshot(snapshotService, testVm.vmId, testVm.projectId, SnapshotType.ON_DEMAND);
    }

    private VmSnapshotResource getVmSnapshotResource() {
        return injector.getInstance(VmSnapshotResource.class);
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

    @Test
    public void testGetSnapshotsFiltersDestroyed() {
        Snapshot snapshot = createTestSnapshot();
        snapshotService.markSnapshotDestroyed(snapshot.id);

        List<Snapshot> snapshots = getVmSnapshotResource().getSnapshotsForVM(snapshot.vmId);
        Assert.assertEquals(0, snapshots.size());
    }

    @Test
    public void testGetSnapshotsFiltersCancelled() {
        Snapshot snapshot = createTestSnapshot();
        snapshotService.updateSnapshotStatus(snapshot.id, SnapshotStatus.CANCELLED);

        List<Snapshot> snapshots = getVmSnapshotResource().getSnapshotsForVM(snapshot.vmId);
        Assert.assertEquals(0, snapshots.size());
    }
}
