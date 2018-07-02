package com.godaddy.vps4.phase2;

import static java.util.UUID.randomUUID;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
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
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmDestroyTest {
    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject SnapshotService snapshotService;
    @Inject ActionService vmActionService;

    private GDUser user;
    private VmSnapshotResource vmSnapshotResource;

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
                    vmSnapshotResource = Mockito.mock(VmSnapshotResource.class);
                    bind(VmSnapshotResource.class).toInstance(vmSnapshotResource);
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
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
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    private long createInProgressAction(UUID vmId, ActionType actionType) {
        UUID commandId = randomUUID();
        Action action = SqlTestData.insertTestVmAction(commandId, vmId, actionType, dataSource);
        vmActionService.markActionInProgress(action.id);
        return action.id;
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
    public void destroyCancelsInProgressActions() throws Exception {
        VirtualMachine vm = createTestVm();
        long inProgressActionId1 = createInProgressAction(vm.vmId, ActionType.STOP_VM);
        long inProgressActionId2 = createInProgressAction(vm.vmId, ActionType.SET_HOSTNAME);

        getVmResource().destroyVm(vm.vmId);
        Assert.assertEquals(vmActionService.getAction(inProgressActionId1).status, ActionStatus.CANCELLED);
        Assert.assertEquals(vmActionService.getAction(inProgressActionId2).status, ActionStatus.CANCELLED);
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
