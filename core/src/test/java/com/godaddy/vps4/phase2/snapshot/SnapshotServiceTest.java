package com.godaddy.vps4.phase2.snapshot;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.snapshot.jdbc.JdbcSnapshotService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;


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

    private void insertTestSnapshots(int count, SnapshotStatus status, SnapshotType snapshotType) {
        for (int i = 0; i < count; i++) {
            Snapshot testSnapshot = new Snapshot(
                    UUID.randomUUID(),
                    vm.projectId,
                    vm.vmId,
                    snapshotName,
                    status,
                    Instant.now(),
                    null,
                    "test-imageid",
                    (int) (Math.random() * 100000),
                    snapshotType
            );
            SqlTestData.insertTestSnapshot(testSnapshot, dataSource);
            snapshotIds.add(testSnapshot.id);
        }
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }


    @Test
    public void testCancelErroredSnapshots() {
        insertTestSnapshots(1, SnapshotStatus.ERROR, SnapshotType.AUTOMATIC);
        Snapshot testSnapshot = snapshotService.getSnapshotsByOrionGuid(vm.orionGuid).get(0);
        insertTestSnapshots(1, SnapshotStatus.ERROR, SnapshotType.ON_DEMAND);
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.AUTOMATIC);
        snapshotService.cancelErroredSnapshots(vm.orionGuid, SnapshotType.AUTOMATIC);
        testSnapshot = snapshotService.getSnapshot(testSnapshot.id);
        assertEquals(SnapshotStatus.CANCELLED, testSnapshot.status);
        List<Snapshot> snapshots = snapshotService.getSnapshotsByOrionGuid(vm.orionGuid);
        assertEquals(3, snapshots.size());
        assertEquals(1, snapshots.stream().filter(snapshot -> snapshot.status == SnapshotStatus.ERROR).collect(Collectors.toList()).size());
        assertEquals(1, snapshots.stream().filter(snapshot -> snapshot.status == SnapshotStatus.LIVE).collect(Collectors.toList()).size());
    }

    @Test
    public void testSnapshotsForUser() {
        insertTestSnapshots(3, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        List<UUID> actualSnapshotIds = snapshotService
                .getSnapshotsForUser(1)
                .stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
        assertThat(actualSnapshotIds, containsInAnyOrder(snapshotIds.toArray()));
    }

    @Test
    public void testGetSnapshot() {
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        for (UUID snapshotId : snapshotIds) {
            Snapshot test = snapshotService.getSnapshot(snapshotId);
            assertEquals(snapshotId, test.id);
        }
    }

    @Test
    public void testSnapshotsForVm() {
        insertTestSnapshots(3, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        List<UUID> actualSnapshotIds = snapshotService
                .getSnapshotsForVm(vm.vmId)
                .stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
        assertThat(actualSnapshotIds, containsInAnyOrder(snapshotIds.toArray()));
    }

    @Test
    public void testCreateSnapshot() {
        UUID snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, snapshotName, SnapshotType.ON_DEMAND);
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
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        assertFalse(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }

    @Test
    public void isNotOverQuotaWhenAllExistingSnapshotsOfSameTypeAreLive() {
        // AUTOMAITIC snapshots are not taken into account when testing if
        // an ON_DEMAND backup is over quota
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.AUTOMATIC);
        assertFalse(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }


    @Test
    public void erroredSnapshotsDontCountTowardsQuotaCheck() {
        insertTestSnapshots(1, SnapshotStatus.ERROR, SnapshotType.ON_DEMAND);
        assertFalse(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }

    @Test
    public void destroyedSnapshotsDontCountTowardsQuotaCheck() {
        insertTestSnapshots(1, SnapshotStatus.DESTROYED, SnapshotType.ON_DEMAND);
        assertFalse(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }

    @Test
    public void isOverQuotaWhenAnyExistingSnapshotIsInNew() {
        // When any existing snapshot linked to an orion guid is not LIVE then the customer is over quota.
        // Hence, quota test should return as not true i.e. isOverQuota? = true
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }

    @Test
    public void isOverQuotaWhenAnyExistingSnapshotIsInDeprecating() {
        // When any existing snapshot linked to an orion guid is not LIVE then the customer is over quota.
        // Hence, quota test should return as not true i.e. isOverQuota? = true
        insertTestSnapshots(1, SnapshotStatus.DEPRECATING, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }

    @Test
    public void isOverQuotaWhenAnyExistingSnapshotIsInDeprecated() {
        // When any existing snapshot linked to an orion guid is not LIVE then the customer is over quota.
        // Hence, quota test should return as not true i.e. isOverQuota? = true
        insertTestSnapshots(1, SnapshotStatus.DEPRECATED, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.isOverQuota(orionGuid, SnapshotType.ON_DEMAND));
    }

    @Test
    public void noOtherBackupInProgressWhenNoOtherBackupExists() {
        // Verify otherBackupsInProgress can handle no other backups existing
        assertFalse(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void noOtherBackupInProgressWhenBackupIsDestroyed() {
        // Destroyed backups do not count towards backups in progress
        insertTestSnapshots(1, SnapshotStatus.DESTROYED, SnapshotType.ON_DEMAND);
        assertFalse(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void noOtherBackupInProgressWhenBackupIsErrored() {
        // Errored backups do not count towards backups in progress
        insertTestSnapshots(1, SnapshotStatus.ERROR, SnapshotType.ON_DEMAND);
        assertFalse(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void otherBackupInProgressWhenBackupIsNew() {
        // Backups in NEW status are already queued, hence a backup is in progress, or soon to
        // be in progress
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void otherBackupInProgressWhenBackupIsInProgress() {
        // A backup in IN_PROGRESS status indicates that a backup is currently running
        insertTestSnapshots(1, SnapshotStatus.IN_PROGRESS, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void otherBackupInProgressWhenBackupIsDeprecating() {
        // When there is a backup in depecating status, that means a backup is currently being taken
        insertTestSnapshots(1, SnapshotStatus.DEPRECATING, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void otherBackupInProgressWhenBackupIsDeprecated() {
        // When a backup is in deprecated status another backup is currently running
        insertTestSnapshots(1, SnapshotStatus.DEPRECATED, SnapshotType.ON_DEMAND);
        assertTrue(snapshotService.otherBackupsInProgress(orionGuid));
    }

    @Test
    public void changeSnapshotStatusToInProgress() {
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotInProgress(snapshotId);
        assertEquals(SnapshotStatus.IN_PROGRESS, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToLive() {
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotLive(snapshotId);
        assertEquals(SnapshotStatus.LIVE, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToErrored() {
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotErrored(snapshotId);
        assertEquals(SnapshotStatus.ERROR, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDestroyed() {
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotDestroyed(snapshotId);
        assertEquals(SnapshotStatus.DESTROYED, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecated() {
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markSnapshotAsDeprecated(snapshotId);
        assertEquals(SnapshotStatus.DEPRECATED, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecatingCase1() {
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid, SnapshotType.ON_DEMAND);
        assertEquals(SnapshotStatus.DEPRECATING, snapshotService.getSnapshot(snapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecatingCase2() {
        // Over here we have 2 snapshots in the DB, however the number of LIVE snapshots
        // equals the number of slots for the account/orionGuid. Hence, we deprecate the
        // LIVE one
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        UUID onDemandSnapshotId = snapshotIds.get(0);
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid, SnapshotType.ON_DEMAND);
        assertEquals(SnapshotStatus.DEPRECATING, snapshotService.getSnapshot(onDemandSnapshotId).status);

    }

    @Test
    public void changeSnapshotStatusToDeprecatingCase3() {
        // Over here we have 3 snapshots in the DB, however the number of LIVE ON_DEMAND snapshots
        // equals the number of slots for the account/orionGuid. Hence, we deprecate the
        // LIVE ON_DEMAND one, but leave the AUTOMATIC one alone
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.AUTOMATIC);
        UUID automaticSnapshotId = snapshotIds.get(0);
        insertTestSnapshots(1, SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        UUID onDemandSnapshotId = snapshotIds.get(1);
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid, SnapshotType.ON_DEMAND);

        assertEquals(SnapshotStatus.LIVE, snapshotService.getSnapshot(automaticSnapshotId).status);
        assertEquals(SnapshotStatus.DEPRECATING, snapshotService.getSnapshot(onDemandSnapshotId).status);
    }

    @Test
    public void changeSnapshotStatusToDeprecatingNoOps() {
        insertTestSnapshots(1, SnapshotStatus.NEW, SnapshotType.ON_DEMAND);
        UUID snapshotId = snapshotIds.get(0);
        snapshotService.markOldestSnapshotForDeprecation(orionGuid, SnapshotType.ON_DEMAND);
        assertEquals(SnapshotStatus.NEW, snapshotService.getSnapshot(snapshotId).status);
    }
}
