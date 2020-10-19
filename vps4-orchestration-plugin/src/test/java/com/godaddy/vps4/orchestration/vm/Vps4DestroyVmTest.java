package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.monitoring.Vps4RemoveMonitoring;
import com.godaddy.vps4.orchestration.scheduler.DeleteAutomaticBackupSchedule;
import com.godaddy.vps4.orchestration.scheduler.ScheduleDestroyVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4DestroyVmTest {

    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    CommandContext context = mock(CommandContext.class);
    VmActionRequest request = mock(VmActionRequest.class);
    VirtualMachine vm = mock(VirtualMachine.class);
    UUID vmId = UUID.randomUUID();
    IpAddress primaryIp = mock(IpAddress.class);

    Vps4DestroyVm command = new Vps4DestroyVm(actionService, networkService);

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());

        vm.vmId = vmId;
        vm.hfsVmId = 42L;
        request.virtualMachine = vm;
        request.actionId = 13L;

        primaryIp.ipAddressId = 23L;
        primaryIp.validUntil = Instant.MAX;
        when(networkService.getVmPrimaryAddress(vm.vmId)).thenReturn(primaryIp);
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
        command.execute(context, request);
        verify(context).execute("RemoveIp-23", Vps4RemoveIp.class, primaryIp);
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
        command.execute(context, request);
        verify(context).execute(eq("MarkIpDeleted-23"), lambda.capture(), eq(Void.class));
        lambda.getValue().apply(context);
        verify(networkService).destroyIpAddress(23);
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
    public void doesNotScheduleRetryIfNoExceptionThrown() {
        command.execute(context, request);
        verify(context, never()).execute(ScheduleDestroyVm.class, vmId);
    }

}
