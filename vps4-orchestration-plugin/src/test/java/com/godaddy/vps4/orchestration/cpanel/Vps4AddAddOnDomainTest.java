package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4AddAddOnDomainTest {
    ActionService actionService = mock(ActionService.class);
    Vps4CpanelService cPanelService = mock(Vps4CpanelService.class);
    CommandContext context = mock(CommandContext.class);
    Vps4AddAddOnDomain.Request req;
    VirtualMachine vm;
    VirtualMachineCredit credit;
    List<String> addOnDomains = Collections.singletonList("test.com");
    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    long hfsVmId = 42L;
    String shopperId = "test-shopper";
    String username = "vpsdev";
    String newDomain = "test.com";
    String runtimeExceptionPrefix = "java.lang.RuntimeException: ";

    @Captor private ArgumentCaptor<Long> hfsVmIdArgumentCaptor;
    @Captor private ArgumentCaptor<String> usernameArgumentCaptor;
    @Captor private ArgumentCaptor<String> newDomainArgumentCaptor;

    Vps4AddAddOnDomain command;
    @Before
    public void setUp() throws CpanelTimeoutException, CpanelAccessDeniedException, IOException {
        // Needed for ActionCommands for thread local ID
        when(context.getId()).thenReturn(UUID.randomUUID());

        vm = mock(VirtualMachine.class);
        vm.vmId = vmId;
        vm.orionGuid = orionGuid;
        vm.hfsVmId = hfsVmId;
        vm.primaryIpAddress = mock(IpAddress.class);

        credit = mock(VirtualMachineCredit.class);
        when(credit.getShopperId()).thenReturn(shopperId);
        when(cPanelService.listAddOnDomains(hfsVmId, username)).thenReturn(addOnDomains);
        when(cPanelService.addAddOnDomain(hfsVmId, username, newDomain)).thenReturn("1");

        req = new Vps4AddAddOnDomain.Request();
        req.actionId = 23L;
        req.vmId = vm.vmId;
        req.hfsVmId = vm.hfsVmId;
        req.username = username;
        req.newDomain = newDomain;

        command = new Vps4AddAddOnDomain(actionService, cPanelService);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void executesValidateConfig() {
        ArgumentCaptor<Vps4ValidateDomainConfig.Request> argument = ArgumentCaptor.forClass(Vps4ValidateDomainConfig.Request.class);

        command.execute(context, req);

        verify(context).execute(eq(Vps4ValidateDomainConfig.class), argument.capture());
        Vps4ValidateDomainConfig.Request request = argument.getValue();
        assertEquals(vmId, request.vmId);
        assertEquals(hfsVmId, (long) request.hfsVmId);
    }

    @Test
    public void callsAddAddonDomainWithCorrectParameters() throws CpanelTimeoutException, CpanelAccessDeniedException {
        command.execute(context, req);

        verify(cPanelService, times(1)).addAddOnDomain(
                hfsVmIdArgumentCaptor.capture(), usernameArgumentCaptor.capture(), newDomainArgumentCaptor.capture());
        Assert.assertEquals(hfsVmId, (long) hfsVmIdArgumentCaptor.getValue());
        Assert.assertEquals(username, usernameArgumentCaptor.getValue());
        Assert.assertEquals(newDomain, newDomainArgumentCaptor.getValue());
    }

    @Test
    public void throwsRuntimeExceptionIfCPanelExceptionOccursDuringAddAddonDomain()
            throws CpanelTimeoutException, CpanelAccessDeniedException {
        String expectedException = runtimeExceptionPrefix + "Could not add an add-on domain to VM: " + vmId;
        when(cPanelService.addAddOnDomain(hfsVmId, username, newDomain)).thenThrow(new CpanelTimeoutException("Test exception"));

        try {
            command.execute(context, req);
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertEquals(expectedException, e.getMessage());
        }
    }

    @Test
    public void passesCPanelFailureReasonToRuntimeException() throws CpanelTimeoutException, CpanelAccessDeniedException {
        String reason = "(XID xbn4z2) The domain test.com already exists in the userdata.";
        String expectedException = runtimeExceptionPrefix + "The cPanel call to add an add-on domain for vmId: "
                + vmId + " has encountered an error: " + reason;
        when(cPanelService.addAddOnDomain(hfsVmId, username, newDomain)).thenReturn(reason);

        try {
            command.execute(context, req);
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertEquals(expectedException, e.getMessage());
        }
    }
}
