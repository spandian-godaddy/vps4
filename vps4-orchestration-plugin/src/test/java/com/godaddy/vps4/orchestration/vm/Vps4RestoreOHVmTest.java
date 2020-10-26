package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.orchestration.hfs.vm.RestoreOHVm;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RestoreOHVmTest {

    ActionService actionService = mock(ActionService.class);
    SnapshotService vps4SnapshotService = mock(SnapshotService.class);
    Vps4RestoreOHVm command = new Vps4RestoreOHVm(actionService, vps4SnapshotService);
    CommandContext context = mock(CommandContext.class);
    Vps4RestoreOHVm.Request request;
    VirtualMachine vm;
    long hfsSnapshotId;

    @Before
    public void setup () {
        UUID vps4SnapshotId = UUID.randomUUID();
        request = new Vps4RestoreOHVm.Request(vps4SnapshotId);
        hfsSnapshotId = 222L;
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.hfsVmId = 111L;
        request.virtualMachine = vm;

        Snapshot snapshot = new Snapshot(
                UUID.randomUUID(),
                333,
                vm.vmId,
                "fake-snapshot",
                SnapshotStatus.LIVE,
                Instant.now().minus(Duration.ofMinutes(10)),
                null,
                "fake-imageid",
                hfsSnapshotId,
                SnapshotType.AUTOMATIC
        );

        when(context.execute(eq("GetHfsSnapshotId"), any(Function.class), eq(
                long.class))).thenReturn(hfsSnapshotId);

        VmAction hfsAction = new VmAction();
        hfsAction.vmActionId = 123L;
        when(context.execute(eq(RestoreOHVm.class), any(RestoreOHVm.Request.class))).thenReturn(hfsAction);
    }

    @Test
    public void testExecuteWithActionCalledRestoreOHVmCmd() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(RestoreOHVm.class), any(RestoreOHVm.Request.class));
    }

    @Test
    public void testExecuteWithActionResponse() {
        Vps4RestoreOHVm.Response response = command.executeWithAction(context, request);
        assertEquals(123L, response.hfsActionId);
        assertEquals(hfsSnapshotId, response.hfsSnapshotId);
        assertEquals(vm.hfsVmId, response.hfsVmId);
    }
}
