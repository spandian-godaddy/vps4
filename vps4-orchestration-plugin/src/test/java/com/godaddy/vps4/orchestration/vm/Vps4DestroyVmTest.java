package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.monitoring.Vps4RemoveMonitoring;
import com.godaddy.vps4.orchestration.scheduler.DeleteAutomaticBackupSchedule;
import com.godaddy.vps4.orchestration.scheduler.ScheduleDestroyVm;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4DestroyVmTest {

    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    ShopperNotesService shopperNotesService = mock(ShopperNotesService.class);
    SnapshotService snapshotService = mock(SnapshotService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CommandContext context = mock(CommandContext.class);
    Vps4DestroyVm.Request request = mock(Vps4DestroyVm.Request.class);
    VirtualMachine vm = mock(VirtualMachine.class);
    UUID vmId = UUID.randomUUID();
    IpAddress primaryIp = mock(IpAddress.class);

    Vps4DestroyVm command = new Vps4DestroyVm(actionService, networkService, shopperNotesService,
                                              snapshotService, virtualMachineService);

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());

        vm.vmId = vmId;
        vm.orionGuid = UUID.randomUUID();
        vm.hfsVmId = 42L;
        request.virtualMachine = vm;
        request.actionId = 13L;
        request.gdUserName = "fake-employee";

        primaryIp.hfsAddressId = 23L;
        primaryIp.validUntil = Instant.MAX;
        when(networkService.getVmPrimaryAddress(vm.vmId)).thenReturn(primaryIp);
    }

    @Test
    public void setsShopperNote() {
        command.execute(context, request);
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(shopperNotesService, times(1))
                .processShopperMessage(eq(vm.vmId), argument.capture());
        String value = argument.getValue();
        Assert.assertTrue(value.contains("destroy"));
        Assert.assertTrue(value.contains(vm.vmId.toString()));
        Assert.assertTrue(value.contains(vm.orionGuid.toString()));
        Assert.assertTrue(value.contains(request.gdUserName));
    }

    @Test
    public void executesUnlicenseControlPanel() {
        command.execute(context, request);
        verify(context).execute(UnlicenseControlPanel.class, vm);
    }

    @Test
    public void executesRemovesMonitoring() {
        command.execute(context, request);
        verify(context).execute(Vps4RemoveMonitoring.class, vm.vmId);
    }

    @Test
    public void executesRemoveIp() {
        primaryIp.addressId = (long) (Math.random() * 100);
        command.execute(context, request);
        verify(context).execute("RemoveIp-" + primaryIp.addressId, Vps4RemoveIp.class, primaryIp);
    }

    @Test
    public void skipsRemoveIpIfNullIp() {
        when(networkService.getVmPrimaryAddress(vm.vmId)).thenReturn(null);
        command.execute(context, request);
        verify(context, never()).execute(any(), eq(Vps4RemoveMonitoring.class), any());
    }

    @Test
    public void skipsRemoveIpIfValidUntilAlreadySet() {
        primaryIp.validUntil = Instant.now().minus(Duration.ofHours(1));
        command.execute(context, request);
        verify(context, never()).execute(any(), eq(Vps4RemoveMonitoring.class), any());
    }

    @Captor ArgumentCaptor<Function<CommandContext, Void>> lambda;
    @Test
    public void marksIpInvalidInDb() {
        MockitoAnnotations.initMocks(this);
        primaryIp.addressId = (long) (Math.random() * 100);
        command.execute(context, request);
        verify(context).execute(eq("MarkIpDeleted-" + primaryIp.addressId), lambda.capture(), eq(Void.class));
        lambda.getValue().apply(context);
        verify(networkService).destroyIpAddress(primaryIp.addressId);
    }

    @Test
    public void deletesAutoBackupSchedule() {
        vm.backupJobId = UUID.randomUUID();
        command.execute(context, request);
        verify(context).execute(DeleteAutomaticBackupSchedule.class, vm.backupJobId);
    }

    @Test
    public void skipsDeleteAutoBackupScheduleIfNullJobId() {
        vm.backupJobId = null;
        command.execute(context, request);
        verify(context, never()).execute(eq(DeleteAutomaticBackupSchedule.class), any());
    }

    @Test
    public void ignoresRuntimeExceptionOnAutoBackupSchedule() {
        vm.backupJobId = UUID.randomUUID();
        when(context.execute(DeleteAutomaticBackupSchedule.class, vm.backupJobId)).thenThrow(new RuntimeException());
        command.execute(context, request);
        verify(context).execute(DeleteAutomaticBackupSchedule.class, vm.backupJobId);
    }

    @Test
    public void deletesAllScheduledJobs(){
        command.execute(context, request);
        verify(context).execute(Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);
    }

    @Test
    public void deletesSupportUsersInDatabase(){
        command.execute(context, request);
        verify(context).execute(Vps4RemoveSupportUsersFromDatabase.class, vm.vmId);
    }

    @Test
    public void deletesHfsVm() {
        ArgumentCaptor<DestroyVm.Request> hfsRequest = ArgumentCaptor.forClass(DestroyVm.Request.class);
        command.execute(context, request);
        verify(context).execute(eq("DestroyVmHfs"), eq(DestroyVm.class), hfsRequest.capture());
        assertEquals(42L, hfsRequest.getValue().hfsVmId);
        assertEquals(13L, hfsRequest.getValue().actionId);
    }

    @Test
    public void returnsResponseWithHfsAction() {
        VmAction hfsAction = new VmAction();
        when(context.execute(eq("DestroyVmHfs"), eq(DestroyVm.class), any(DestroyVm.Request.class)))
                .thenReturn(hfsAction);
        Vps4DestroyVm.Response response = command.execute(context, request);
        assertEquals(vmId, response.vmId);
        assertEquals(hfsAction, response.hfsAction);
    }

    @Test
    public void schedulesRetryIfExceptionThrown() {
        when(context.execute(eq("DestroyVmHfs"), eq(DestroyVm.class), any(DestroyVm.Request.class)))
                .thenThrow(new RuntimeException("Faked HFS failure"));
        try {
            command.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            verify(context).execute(ScheduleDestroyVm.class, vmId);
        }
    }

    @Test
    public void schedulesRetryIfCreateIsInProgress() {
        Action createAction = mock(Action.class);
        createAction.type = ActionType.CREATE_VM;
        when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(createAction));
        try {
            command.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            assert e.getMessage().contains("Create action is already running");
            verify(context).execute(ScheduleDestroyVm.class, vmId);
        }
    }

    @Test
    public void doesNotScheduleRetryIfNoExceptionThrown() {
        command.execute(context, request);
        verify(context, never()).execute(ScheduleDestroyVm.class, vmId);
    }


    @Test
    public void executesRemoveIpForAdditionalIps() {
        IpAddress primaryIp = new IpAddress(1,1111, vmId, "1.2.3.4", IpAddress.IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), 4);
        IpAddress secondaryIp = new IpAddress(2,1112, vmId, "1.2.3.4", IpAddress.IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), 4);
        IpAddress removedIp = new IpAddress(3,1113, vmId, "1.2.3.4", IpAddress.IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), 4);

        List<IpAddress> secondaryIps = new ArrayList<IpAddress>();
        secondaryIps.add(primaryIp);
        secondaryIps.add(secondaryIp);
        secondaryIps.add(removedIp);
        when(networkService.getVmSecondaryAddress(vm.hfsVmId)).thenReturn(secondaryIps);
        command.execute(context, request);
        for (IpAddress ip : secondaryIps) {
            verify(context).execute(eq("RemoveIp-" + ip.addressId), eq(Vps4RemoveIp.class), any());
        }
    }

    @Test
    public void marksIpDeletedForAdditionalIps() {
        IpAddress primaryIp = new IpAddress(1,1111, vmId, "1.2.3.4", IpAddress.IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), 4);
        IpAddress secondaryIp = new IpAddress(2,1112, vmId, "1.2.3.4", IpAddress.IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), 4);
        IpAddress removedIp = new IpAddress(3,1113, vmId, "1.2.3.4", IpAddress.IpAddressType.SECONDARY,
                null, Instant.now(), Instant.now().minus(24, ChronoUnit.HOURS), 4);

        MockitoAnnotations.initMocks(this);
        List<IpAddress> secondaryIps = new ArrayList<IpAddress>();
        secondaryIps.add(primaryIp);
        secondaryIps.add(secondaryIp);
        secondaryIps.add(removedIp);
        when(networkService.getVmSecondaryAddress(vm.hfsVmId)).thenReturn(secondaryIps);
        command.execute(context, request);
        for (IpAddress ip : secondaryIps) {
            verify(context).execute(eq("MarkIpDeleted-" + ip.addressId), lambda.capture(), eq(Void.class));
            lambda.getValue().apply(context);
            verify(networkService).destroyIpAddress(ip.addressId);
        }
    }

    @Test
    public void doesNotRunDeleteAdditionalIpsIfThereAreNone() {
        when(networkService.getVmSecondaryAddress(vm.hfsVmId)).thenReturn(null);
        command.execute(context, request);
        verify(context, times(1)).execute(startsWith("RemoveIp-"), eq(Vps4RemoveIp.class), any());
        verify(context, times(1)).execute(startsWith("MarkIpDeleted-"),
                                         Matchers.<Function<CommandContext, Void>> any(),
                                         eq(Void.class));
        verify(networkService, never()).destroyIpAddress(anyLong());
    }

    @Test
    public void unclaimsCredit() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(Vps4UnclaimCredit.class), isA(VirtualMachine.class));
    }

    @Captor ArgumentCaptor<Function<CommandContext, Void>> deleteVmCapture;
    @Test
    public void deletesVmInDb() {
        MockitoAnnotations.initMocks(this);
        command.execute(context, request);
        verify(context, times(1)).execute(eq("MarkVmDeleted"), deleteVmCapture.capture(), eq(Void.class));
        deleteVmCapture.getValue().apply(context);
        verify(virtualMachineService, times(1)).setVmRemoved(vmId);
    }

    @Test
    public void cancelsIncompleteActions() {
        Action a1 = mock(Action.class);
        Action a2 = mock(Action.class);
        a1.id = 5;
        a1.type = ActionType.ADD_MONITORING;
        a2.id = 7;
        a2.type = ActionType.REMOVE_SUPPORT_USER;
        List<Action> actions = Arrays.asList(a1, a2);
        when(actionService.getIncompleteActions(vmId)).thenReturn(actions);
        command.execute(context, request);
        verify(actionService, times(1)).getIncompleteActions(vmId);
        verify(context, times(1)).execute(eq("MarkActionCancelled-5"),
                                          Matchers.<Function<CommandContext, Void>> any(),
                                          eq(Void.class));
        verify(context, times(1)).execute(eq("MarkActionCancelled-7"),
                                          Matchers.<Function<CommandContext, Void>> any(),
                                          eq(Void.class));
    }

    @Test
    public void doesNotCancelCurrentDestroyAction() {
        Action action = mock(Action.class);
        action.id = request.actionId;
        action.type = ActionType.DESTROY_VM;
        List<Action> actions = Collections.singletonList(action);
        when(actionService.getIncompleteActions(vmId)).thenReturn(actions);
        command.execute(context, request);
        verify(actionService, times(1)).getIncompleteActions(vmId);
        verify(context, never()).execute(eq("MarkActionCancelled-" + request.actionId),
                                          Matchers.<Function<CommandContext, Void>> any(),
                                          eq(Void.class));
    }

    @Test(expected = RuntimeException.class)
    public void reschedulesDestroyIfCreateActionInProgress() {
        Action action = mock(Action.class);
        action.type = ActionType.CREATE_VM;
        List<Action> actions = Collections.singletonList(action);
        when(actionService.getIncompleteActions(vmId)).thenReturn(actions);
        command.execute(context, request);
        verify(actionService, times(1)).getIncompleteActions(vmId);
        verify(context, never()).execute(startsWith("MarkActionCancelled-"),
                                         Matchers.<Function<CommandContext, Void>> any(),
                                         eq(Void.class));
        verify(context, times(1)).execute(ScheduleDestroyVm.class, vmId);
    }

    private Snapshot createFakeSnapshot(SnapshotStatus status) {
        return new Snapshot(UUID.randomUUID(), 0L, UUID.randomUUID(), "fake-snapshot", status,
                            Instant.now(), null, "fake-image-id", (long) (Math.random() * 100000),
                            SnapshotType.AUTOMATIC);
    }

    @Test
    public void marksErroredSnapshotsAsCancelled() {
        Snapshot s1 = createFakeSnapshot(SnapshotStatus.ERROR);
        Snapshot s2 = createFakeSnapshot(SnapshotStatus.LIVE);
        List<Snapshot> snapshots = Arrays.asList(s1, s2);
        when(snapshotService.getSnapshotsForVm(vmId)).thenReturn(snapshots);
        command.execute(context, request);
        verify(snapshotService, times(1)).getSnapshotsForVm(vmId);
        verify(snapshotService, times(1)).updateSnapshotStatus(s1.id, SnapshotStatus.CANCELLED);
        verify(snapshotService, never()).updateSnapshotStatus(s2.id, SnapshotStatus.CANCELLED);
    }

    @Test
    public void destroysSnapshots() {
        Snapshot s1 = createFakeSnapshot(SnapshotStatus.ERROR);
        Snapshot s2 = createFakeSnapshot(SnapshotStatus.LIVE);
        List<Snapshot> snapshots = Arrays.asList(s1, s2);
        when(snapshotService.getSnapshotsForVm(vmId)).thenReturn(snapshots);
        command.execute(context, request);
        verify(snapshotService, times(1)).getSnapshotsForVm(vmId);
        verify(context, never()).execute(eq("Vps4DestroySnapshot-" + s1.id),
                                         eq(Vps4DestroySnapshot.class),
                                         any(Vps4DestroySnapshot.Request.class));
        verify(context, times(1)).execute(eq("Vps4DestroySnapshot-" + s2.id),
                                          eq(Vps4DestroySnapshot.class),
                                          any(Vps4DestroySnapshot.Request.class));
    }
}
