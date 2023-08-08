package com.godaddy.vps4.orchestration.vm;

import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField.PLAN_CHANGE_PENDING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

import java.util.UUID;
import java.util.function.Function;

import com.godaddy.vps4.vm.Image;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.ResizeOHVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import org.mockito.ArgumentCaptor;

public class Vps4UpgradeOHVmTest {

    ActionService actionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    Vps4UpgradeOHVm command = new Vps4UpgradeOHVm(actionService, virtualMachineService, creditService);
    CommandContext context = mock(CommandContext.class);
    Vps4UpgradeOHVm.Request request;
    VirtualMachine vm;

    @Before
    public void setup () {
        request = new Vps4UpgradeOHVm.Request();
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.hfsVmId = 111L;
        vm.image = mock(Image.class);
        vm.image.operatingSystem = Image.OperatingSystem.LINUX;
        request.virtualMachine = vm;
        request.newTier = 41;

        ServerSpec newSpec = new ServerSpec();
        newSpec.specName = "oh.hosting.c4.r16.d200";
        newSpec.specId = 88;
        when(virtualMachineService.getSpec(request.newTier, ServerType.Platform.OPTIMIZED_HOSTING.getplatformId())).thenReturn(newSpec);

        VmAction hfsAction = new VmAction();
        hfsAction.vmActionId = 123L;
        when(context.execute(eq(ResizeOHVm.class), any(ResizeOHVm.Request.class))).thenReturn(hfsAction);
    }

    @Test
    public void testAppendsCtForImportedContainerWhenExecutingRestoreVmCommand() {
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(virtualMachineService.getImportedVm(vm.vmId)).thenReturn(vm.vmId);
        ArgumentCaptor<ResizeOHVm.Request> argument = ArgumentCaptor.forClass(ResizeOHVm.Request.class);

        command.executeWithAction(context, request);

        verify(virtualMachineService, atLeastOnce()).getImportedVm(vm.vmId);
        verify(virtualMachineService, atLeastOnce()).getSpec(request.newTier, ServerType.Platform.OPTIMIZED_HOSTING.getplatformId());

        verify(context).execute(eq(ResizeOHVm.class), argument.capture());
        ResizeOHVm.Request restoreReq = argument.getValue();
        assertEquals(restoreReq.newSpecName, "oh.hosting.c4.r16.d200.ct");
    }

    @Test
    public void testDoesNotAppendCtForImportedWindowsVMs() {
        vm.image.operatingSystem = Image.OperatingSystem.WINDOWS;
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(virtualMachineService.getImportedVm(vm.vmId)).thenReturn(vm.vmId);
        ArgumentCaptor<ResizeOHVm.Request> argument = ArgumentCaptor.forClass(ResizeOHVm.Request.class);

        command.executeWithAction(context, request);

        verify(virtualMachineService, atLeastOnce()).getImportedVm(vm.vmId);
        verify(virtualMachineService, atLeastOnce()).getSpec(request.newTier, ServerType.Platform.OPTIMIZED_HOSTING.getplatformId());

        verify(context).execute(eq(ResizeOHVm.class), argument.capture());
        ResizeOHVm.Request restoreReq = argument.getValue();
        assertEquals(restoreReq.newSpecName, "oh.hosting.c4.r16.d200");
    }

    @Test
    public void testCalledUpgradeOHVmCmd() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(ResizeOHVm.class), any(ResizeOHVm.Request.class));
    }

    @Test
    public void updateVmTierInDb() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq("UpdateVmTier"), any(Function.class), eq(Void.class));
    }

    @Test
    public void testUpdateEcommCredit() {
        command.executeWithAction(context, request);
        verify(creditService,times(1)).updateProductMeta(eq(vm.orionGuid), eq(PLAN_CHANGE_PENDING), eq("false"));
    }
}
