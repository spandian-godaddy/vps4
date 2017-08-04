package com.godaddy.vps4.phase2.snapshot;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotWithDetails;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.snapshot.jdbc.JdbcSnapshotService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;


public class SnapshotServiceTest {

    static final String snapshotName = "core-snapshot";
    private SnapshotService snapshotService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;
    private VirtualMachine vm;
    private List<UUID> snapshotIds;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        snapshotService = new JdbcSnapshotService(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        snapshotIds = new ArrayList<>();
    }

    private void insertTestSnapshots(int count, SnapshotStatus status) {
        for (int i = 0; i < count; i++) {
            SnapshotWithDetails testSnapshot = new SnapshotWithDetails(UUID.randomUUID(), "test",
                    vm.projectId, (int) (Math.random() * 100000), vm.vmId, snapshotName,
                    status, Instant.now(), null );
            SqlTestData.insertTestSnapshot(testSnapshot, dataSource);
            snapshotIds.add(testSnapshot.id);
        }
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void testSnapshotsForUser() {
        insertTestSnapshots(3, SnapshotStatus.LIVE);
        List<UUID> actualSnapshotIds = snapshotService
                .getSnapshotsForUser(1)
                .stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
        assertThat(actualSnapshotIds, containsInAnyOrder(snapshotIds.toArray()));
    }

    @Test
    public void testGetSnapshot() {
        insertTestSnapshots(1, SnapshotStatus.LIVE);
        for (UUID snapshotId : snapshotIds) {
            Snapshot test = snapshotService.getSnapshot(snapshotId);
            assertEquals(snapshotId, test.id);
        }
    }

    @Test
    public void testGetSnapshotWithDetails() {
        insertTestSnapshots(1, SnapshotStatus.LIVE);
        for (UUID snapshotId : snapshotIds) {
            SnapshotWithDetails testWithDetails = snapshotService.getSnapshotWithDetails(snapshotId);
            assertEquals(snapshotId, testWithDetails.id);
        }
    }

    @Test
    public void testSnapshotsForVm() {
        insertTestSnapshots(3, SnapshotStatus.LIVE);
        List<UUID> actualSnapshotIds = snapshotService
                .getSnapshotsForVm(vm.vmId)
                .stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
        assertThat(actualSnapshotIds, containsInAnyOrder(snapshotIds.toArray()));
    }

    @Test
    public void testCreateSnapshot() {
        UUID snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, snapshotName);
        List<UUID> actualSnapshotIds = snapshotService
                .getSnapshotsForVm(vm.vmId)
                .stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
        assertTrue(actualSnapshotIds.contains(snapshotId));
        assertEquals(SnapshotStatus.NEW, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void isNotOverQuotaWhenAllExistingSnapshotsAreLive() {
        // When all the existing snapshot linked to an orion guid are LIVE then the oldest can be deprecated.
        // Hence, quota test should return as false i.e. isOverQuota? = false
        insertTestSnapshots(1, SnapshotStatus.LIVE);
        assertFalse(snapshotService.isOverQuota(orionGuid));
    }

    @Test
    public void erroredSnapshotsDontCountTowardsQuotaCheck() {
        insertTestSnapshots(1, SnapshotStatus.ERROR);
        assertFalse(snapshotService.isOverQuota(orionGuid));
    }

    @Test
    public void destroyedSnapshotsDontCountTowardsQuotaCheck() {
        insertTestSnapshots(1, SnapshotStatus.DESTROYED);
        assertFalse(snapshotService.isOverQuota(orionGuid));
    }

    @Test
    public void isOverQuotaWhenAnyExistingSnapshotIsInNew() {
        // When any existing snapshot linked to an orion guid is not LIVE then the customer is over quota.
        // Hence, quota test should return as not true i.e. isOverQuota? = true
        insertTestSnapshots(1, SnapshotStatus.NEW);
        assertTrue(snapshotService.isOverQuota(orionGuid));
    }

    @Test
    public void isOverQuotaWhenAnyExistingSnapshotIsInDeprecating() {
        // When any existing snapshot linked to an orion guid is not LIVE then the customer is over quota.
        // Hence, quota test should return as not true i.e. isOverQuota? = true
        insertTestSnapshots(1, SnapshotStatus.DEPRECATING);
        assertTrue(snapshotService.isOverQuota(orionGuid));
    }

    @Test
    public void isOverQuotaWhenAnyExistingSnapshotIsInDeprecated() {
        // When any existing snapshot linked to an orion guid is not LIVE then the customer is over quota.
        // Hence, quota test should return as not true i.e. isOverQuota? = true
        insertTestSnapshots(1, SnapshotStatus.DEPRECATED);
        assertTrue(snapshotService.isOverQuota(orionGuid));
    }

    @Test
    public void changeSnapshotStatusToInProgress() {
        insertTestSnapshots(1, SnapshotStatus.NEW);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotInProgress(snapshotId);
        assertEquals(SnapshotStatus.IN_PROGRESS, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToLive() {
        insertTestSnapshots(1, SnapshotStatus.NEW);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotLive(snapshotId);
        assertEquals(SnapshotStatus.LIVE, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToErrored() {
        insertTestSnapshots(1, SnapshotStatus.NEW);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotErrored(snapshotId);
        assertEquals(SnapshotStatus.ERROR, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDestroyed() {
        insertTestSnapshots(1, SnapshotStatus.NEW);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotDestroyed(snapshotId);
        assertEquals(SnapshotStatus.DESTROYED, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecated() {
        insertTestSnapshots(1, SnapshotStatus.NEW);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotAsDeprecated(snapshotId);
        assertEquals(SnapshotStatus.DEPRECATED, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecatingCase1() {
        insertTestSnapshots(1, SnapshotStatus.LIVE);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid);
        assertEquals(SnapshotStatus.DEPRECATING, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecatingCase2() {
        // Over here we have 2 snapshots in the DB, however the number of LIVE snapshots
        // equals the number of slots for the account/orionGuid. Hence, we deprecate the
        // LIVE one
        insertTestSnapshots(1, SnapshotStatus.LIVE);
        UUID snapshotId = snapshotIds.get(0);
        insertTestSnapshots(1, SnapshotStatus.NEW);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid);
        assertEquals(SnapshotStatus.DEPRECATING, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecatingNoOps() {
        insertTestSnapshots(1, SnapshotStatus.NEW);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid);
        assertEquals(SnapshotStatus.NEW, snapshotService.getSnapshot(snapshotId).status);
    }
}
