package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.panopta.AddAdditionalFqdnPanopta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4AddDomainMonitoringTest {

    ActionService actionService = mock(ActionService.class);
    CommandContext context = mock(CommandContext.class);
    Vps4AddDomainMonitoring.Request req;
    VirtualMachine vm;
    VirtualMachineCredit credit;

    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    long hfsVmId = 42L;
    String shopperId = "test-shopper";

    Vps4AddDomainMonitoring command = new Vps4AddDomainMonitoring(actionService);

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

        req = new Vps4AddDomainMonitoring.Request();
        req.actionId = 23L;
        req.vmId = vmId;
        req.osTypeId = 1;
        req.additionalFqdn = "thisfqdn.isdefinitely.fake";
    }

    @Test
    public void executesAddAdditionalFqdnPanopta() {
        command.execute(context, req);

        ArgumentCaptor<AddAdditionalFqdnPanopta.Request> argument = ArgumentCaptor.forClass(AddAdditionalFqdnPanopta.Request.class);
        verify(context).execute(eq(AddAdditionalFqdnPanopta.class), argument.capture());
    }


    @Test
    public void executesAddAdditionalFqdnPanoptaFakeFqdn() {
        command.execute(context, req);

        ArgumentCaptor<AddAdditionalFqdnPanopta.Request> argument = ArgumentCaptor.forClass(AddAdditionalFqdnPanopta.Request.class);
        verify(context).execute(eq(AddAdditionalFqdnPanopta.class), argument.capture());
        AddAdditionalFqdnPanopta.Request request = argument.getValue();
        assertEquals(vmId, request.vmId);
        assertEquals(1, request.operatingSystemId);
        assertEquals("thisfqdn.isdefinitely.fake", request.additionalFqdn);
        assertEquals(false, request.isHttps);
    }


    @Test
    public void executesAddAdditionalFqdnPanoptaValidFqdn() {
        req.additionalFqdn = "myh.godaddy.com";
        command.execute(context, req);

        ArgumentCaptor<AddAdditionalFqdnPanopta.Request> argument = ArgumentCaptor.forClass(AddAdditionalFqdnPanopta.Request.class);
        verify(context).execute(eq(AddAdditionalFqdnPanopta.class), argument.capture());
        AddAdditionalFqdnPanopta.Request request = argument.getValue();
        assertEquals(vmId, request.vmId);
        assertEquals(1, request.operatingSystemId);
        assertEquals("myh.godaddy.com", request.additionalFqdn);
        assertEquals(true, request.isHttps);
    }
}