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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class SnapshotServiceTest {

    private SnapshotService snapshotService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;
    private VirtualMachine vm;
    private List<Snapshot> snapshots;
    private List<SnapshotWithDetails> snapshotsWithDetails;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        snapshotService = new JdbcSnapshotService(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        snapshots = new ArrayList<>();
        snapshotsWithDetails = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SnapshotWithDetails testSnapshot = new SnapshotWithDetails(
                    UUID.randomUUID(),
                    "test",
                    vm.projectId,
                    (int) (Math.random() * 100000),
                    vm.vmId,
                    "test-snapshot",
                    SnapshotStatus.COMPLETE,
                    Instant.now(),
                    null
            );
            snapshots.add(new Snapshot(
                    testSnapshot.id,
                    testSnapshot.projectId,
                    testSnapshot.vmId,
                    testSnapshot.name,
                    testSnapshot.status,
                    Instant.now(),
                    null
            ));
            snapshotsWithDetails.add(testSnapshot);
            SqlTestData.insertTestSnapshot(testSnapshot, dataSource);
        }
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void testSnapshotsForUser() {
        String test = snapshotService.getSnapshotsForUser(1).toString();
        for (Snapshot snapshot : snapshots) {
            if (!test.contains(snapshot.id.toString()))
                fail();
        }
    }

    @Test
    public void testGetSnapshot() {
        for (Snapshot snapshot : snapshots) {
            Snapshot test = snapshotService.getSnapshot(snapshot.id);
            assertEquals(snapshot.id, test.id);
        }
    }

    @Test
    public void testGetSnapshotWithDetails() {
        for (SnapshotWithDetails snapshot : snapshotsWithDetails) {
            SnapshotWithDetails testWithDetails = snapshotService.getSnapshotWithDetails(snapshot.id);
            assertEquals(snapshot.id, testWithDetails.id);
        }
    }

    @Test
    public void testSnapshotsForVm() {
        String test = snapshotService.getSnapshotsForVm(vm.vmId).toString();
        for (Snapshot snapshot : snapshots) {
            if (!test.contains(snapshot.id.toString()))
                fail();
        }
    }
}
