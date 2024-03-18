package com.godaddy.vps4.orchestration.vm;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.nydus.UpgradeNydus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class Vps4SyncOsStatusTest {
    @Mock private ActionService actionService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private VmService vmService;
    @Mock private CommandContext context;
    @Mock private VirtualMachine vm;
    @Mock private VmAction hfsAction;
    @Mock private VmActionRequest request;

    @Captor private ArgumentCaptor<Function<CommandContext, VmAction>> vmActionCommandCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Void>> voidCommandCaptor;

    Vps4SyncOsStatus command;

    @Before
    public void setUp() {
        hfsAction.resultset = "{\"operating_system\":\"almalinux-8\",\"os_upgraded\":true}";

        vm.hfsVmId = 1234L;
        vm.vmId = UUID.randomUUID();
        request.virtualMachine = vm;

        command = new Vps4SyncOsStatus(actionService, virtualMachineService, vmService);

        when(context.execute(eq("Vps4SyncOsStatus"), any(Function.class), eq(VmAction.class))).thenReturn(hfsAction);
        when(context.execute(eq(WaitForManageVmAction.class), any(VmAction.class))).thenReturn(hfsAction);
    }

    @Test
    public void callsSyncOsEndpoint() {
        command.executeWithAction(context, request);

        verify(context, times(1)).execute(eq("Vps4SyncOsStatus"), vmActionCommandCaptor.capture(), eq(VmAction.class));
        vmActionCommandCaptor.getValue().apply(context);
        verify(vmService, times(1)).syncOs(vm.hfsVmId, "osinfo");
    }

    @Test
    public void waitsForManageVmAction() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(WaitForManageVmAction.class, hfsAction);
    }

    @Test
    public void updatesCurrentOsIfUpgraded() {
        command.executeWithAction(context, request);

        verify(context, times(1)).execute(eq("UpdateCurrentOs"), voidCommandCaptor.capture(), eq(Void.class));
        voidCommandCaptor.getValue().apply(context);
        verify(virtualMachineService, times(1)).setCurrentOs(vm.vmId, "almalinux-8");
    }

    @Test
    public void doesNotUpdateCurrentOsIfNotUpgraded() {
        hfsAction.resultset = "{\"operating_system\":\"almalinux-8\",\"os_upgraded\":false}";

        command.executeWithAction(context, request);

        verify(context, never()).execute(eq("UpdateCurrentOs"), any(Function.class), eq(Void.class));
    }

    @Test
    public void callsUpgradeNydusIfUpgraded() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(UpgradeNydus.class), any(UpgradeNydus.Request.class));
    }

    @Test
    public void doesNotCallUpgradeNydusIfNotUpgraded() {
        hfsAction.resultset = "{\"operating_system\":\"almalinux-8\",\"os_upgraded\":false}";

        command.executeWithAction(context, request);
        verify(context, never()).execute(eq(UpgradeNydus.class), any(UpgradeNydus.Request.class));
    }
}
