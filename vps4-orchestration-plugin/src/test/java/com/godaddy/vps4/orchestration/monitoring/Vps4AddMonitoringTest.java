package com.godaddy.vps4.orchestration.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4AddMonitoringTest {

    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VmActionRequest req;
    VirtualMachine vm;
    VirtualMachineCredit credit;

    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    long hfsVmId = 42L;
    String shopperId = "test-shopper";

    Vps4AddMonitoring command = new Vps4AddMonitoring(actionService, creditService);

    @Before
    public void setUp() {
        // Needed for ActionCommands for thread local id
        when(context.getId()).thenReturn(UUID.randomUUID());

        vm = mock(VirtualMachine.class);
        vm.vmId = vmId;
        vm.orionGuid = orionGuid;
        vm.hfsVmId = hfsVmId;
        vm.primaryIpAddress = mock(IpAddress.class);

        credit = mock(VirtualMachineCredit.class);
        when(credit.getShopperId()).thenReturn(shopperId);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        req = new VmActionRequest();
        req.actionId = 23L;
        req.virtualMachine = vm;
    }

    @Test
    public void executesSetupPanopta() {
        command.execute(context, req);

        ArgumentCaptor<SetupPanopta.Request> argument = ArgumentCaptor.forClass(SetupPanopta.Request.class);
        verify(context).execute(eq(SetupPanopta.class), argument.capture());
        SetupPanopta.Request request = argument.getValue();
        assertEquals(vmId, request.vmId);
        assertEquals(orionGuid, request.orionGuid);
        assertEquals(hfsVmId, request.hfsVmId);
        assertEquals(shopperId, request.shopperId);
    }

    @Test
    public void executesRemoveNodePing() {
        vm.primaryIpAddress.pingCheckId = 13L;
        command.execute(context, req);

        verify(context).execute(RemoveNodePingMonitoring.class, vm.primaryIpAddress);
    }

    @Test
    public void skipsRemoveNodePingIfNullIp() {
        vm.primaryIpAddress = null;
        command.execute(context, req);

        verify(context, never()).execute(eq(RemoveNodePingMonitoring.class), any());
    }

    @Test
    public void skipsRemoveNodePingIfPingCheckIdNull() {
        vm.primaryIpAddress.pingCheckId = null;
        command.execute(context, req);

        verify(context, never()).execute(eq(RemoveNodePingMonitoring.class), any());
    }
}
