package com.godaddy.vps4.phase2;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.GDUser;
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

import javax.inject.Inject;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmDestroyTest {
    @Inject
    Vps4UserService userService;
    @Inject
    DataSource dataSource;
    @Inject
    ActionService vmActionService;

    private GDUser user;
    private final VmSnapshotResource vmSnapshotResource = Mockito.mock(VmSnapshotResource.class);
    private final SnapshotService snapshotService = Mockito.mock(SnapshotService.class);
    private final MailRelayService mailRelayService = mock(MailRelayService.class);

    private Injector injector = Guice.createInjector(new DatabaseModule(), new SecurityModule(), new VmModule(),
            new Phase2ExternalsModule(), new CancelActionModule(), new AbstractModule() {

                @Override
                public void configure() {
                    bind(VmSnapshotResource.class).toInstance(vmSnapshotResource);
                    bind(SnapshotService.class).toInstance(snapshotService);
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(MailRelayService.class).toInstance(mailRelayService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private VirtualMachine createTestVm() {
        UUID orionGuid = randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1", UUID.randomUUID());
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        SqlTestData.insertTestIp(vm.hfsVmId, vm.vmId, "192.168.0.112", IpAddress.IpAddressType.PRIMARY, dataSource);
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
        when(mailRelayService.getMailRelay(anyString())).thenReturn(new MailRelay());
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private Snapshot createSnapshot(UUID vmId, SnapshotStatus status){
        UUID snapshotId = UUID.randomUUID();
        long projectId = 1;
        String name = "fakename";
        Instant createdAt = Instant.now();
        Instant modifiedAt = null;
        String hfsImageId = null;
        long hfsSnapshotId = 0;
        SnapshotType snapshotType = SnapshotType.AUTOMATIC;

        return new Snapshot(snapshotId, projectId, vmId, name, status,
                createdAt, modifiedAt, hfsImageId, hfsSnapshotId, snapshotType);
    }

    @Test
    public void destroySkipsNewErroredAndRescheduledSnapshots() throws Exception {
        VirtualMachine vm = createTestVm();

        Snapshot erroredSnapshot = createSnapshot(vm.vmId, SnapshotStatus.ERROR);
        Snapshot newSnapshot = createSnapshot(vm.vmId, SnapshotStatus.NEW);
        Snapshot rescheduledSnapshot = createSnapshot(vm.vmId, SnapshotStatus.ERROR_RESCHEDULED);

        List<Snapshot> snapshots = Arrays.asList(erroredSnapshot, newSnapshot, rescheduledSnapshot);

        when(getVmSnapshotResource().getSnapshotsForVM(Mockito.any())).thenReturn(snapshots);

        VmAction vmAction = getVmResource().destroyVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
        verify(vmSnapshotResource, times(0)).destroySnapshot(eq(vm.vmId), any(UUID.class));
    }
}
