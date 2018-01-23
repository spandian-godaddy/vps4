package com.godaddy.vps4.phase2;

import static java.util.UUID.randomUUID;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.scheduler.api.client.SchedulerServiceClientModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmAction;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class VmDestroyTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject SnapshotService snapshotService;

    private GDUser user;
    private VmSnapshotResource vmSnapshotResource;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new SchedulerServiceClientModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    vmSnapshotResource = Mockito.mock(VmSnapshotResource.class);
                    bind(VmSnapshotResource.class).toInstance(vmSnapshotResource);

                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private List<Snapshot> createSnapshots(int numberOfSnapshots, VirtualMachine vm) {
        List<Snapshot> snapshots = new ArrayList<>();
        while (numberOfSnapshots > 0) {
            Snapshot snapshot = SqlTestData.insertSnapshot(snapshotService, vm.vmId, vm.projectId, SnapshotType.ON_DEMAND);
            snapshots.add(snapshot);
            numberOfSnapshots--;
        }
        return snapshots;
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    private VmResource getVmResource() {
        return injector.getInstance(VmResource.class);
    }

    private VmSnapshotResource getVmSnapshotResource() {
        return injector.getInstance(VmSnapshotResource.class);
    }

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    @Test
    public void destroyTestVmWithSnapshot() throws Exception {
        int numberOfSnapshots = 1;
        VirtualMachine vm = createTestVm();
        List<Snapshot> snapshots = createSnapshots(numberOfSnapshots, vm);

        Mockito.when(getVmSnapshotResource().getSnapshotsForVM(Mockito.any())).thenReturn(snapshots);

        VmAction vmAction = getVmResource().destroyVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
    }

    @Test
    public void destroyTestVmWithSnapshots() throws Exception {
        int numberOfSnapshots = 5;
        VirtualMachine vm = createTestVm();
        List<Snapshot> snapshots = createSnapshots(numberOfSnapshots, vm);

        Mockito.when(getVmSnapshotResource().getSnapshotsForVM(Mockito.any())).thenReturn(snapshots);

        VmAction vmAction = getVmResource().destroyVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
    }

}