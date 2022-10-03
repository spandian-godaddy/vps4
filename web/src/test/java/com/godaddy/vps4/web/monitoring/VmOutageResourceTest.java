package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmOutageResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private CommandService commandService = mock(CommandService.class);
    private CreditService creditService = mock(CreditService.class);
    private PanoptaService panoptaService = mock(PanoptaService.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private ActionService actionService = mock(ActionService.class);
    private GDUser gdUser = mock(GDUser.class);

    private VmOutageResource resource = new VmOutageResource(vmResource, commandService, creditService, panoptaService, actionService,
            gdUser);
    private UUID vmId = UUID.randomUUID();
    private VmMetricAlert vmMetricAlert = new VmMetricAlert();
    private VirtualMachine vm;
    private int outageId = 23;
    private String shopperId = "fake-shopper-id";
    long actionId = 123321;

    private final ArgumentCaptor<CommandGroupSpec> commandCapture = ArgumentCaptor.forClass(CommandGroupSpec.class);

    @Before
    public void setUp() {
        vm = new VirtualMachine();
        vm.hostname = "TestHostname";
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.primaryIpAddress = new IpAddress();
        vm.primaryIpAddress.ipAddress = "127.0.0.1";
        vm.validUntil = Instant.MAX;
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);

        when(creditService.getVirtualMachineCredit(eq(vm.orionGuid))).thenReturn(credit);
        when(credit.isManaged()).thenReturn(false);
        when(credit.isAccountActive()).thenReturn(true);

        VmOutage vmOutage = new VmOutage();
        vmOutage.metrics = Collections.singleton(VmMetric.CPU);

        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(gdUser.getShopperId()).thenReturn(shopperId);
        when(gdUser.isAdmin()).thenReturn(true);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        Action action = mock(Action.class);
        when(actionService.createAction(vmId, ActionType.NEW_VM_OUTAGE, new JSONObject().toJSONString(), gdUser.getUsername())).thenReturn(actionId);
        when(actionService.createAction(vmId, ActionType.CLEAR_VM_OUTAGE, new JSONObject().toJSONString(), gdUser.getUsername())).thenReturn(actionId);
        when(actionService.getAction(actionId)).thenReturn(action);

        PanoptaServer panoptaServer = mock(PanoptaServer.class);
        panoptaServer.partnerCustomerKey = "gdtest_" + shopperId;
        when(panoptaService.getServer(vmId)).thenReturn(panoptaServer);
    }

    @Test
    public void getOutageList() throws PanoptaServiceException {
        resource.getVmOutageList(vmId, false);
        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutages(vmId, false);
    }

    @Test
    public void getActiveFilteredOutageList() throws PanoptaServiceException {
        resource.getVmOutageList(vmId, true);
        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutages(vmId, true);
    }

    @Test
    public void createOutage() throws PanoptaServiceException {
        resource.newVmOutage(vmId, outageId);
        verify(commandService).executeCommand(commandCapture.capture());
        assertEquals("Vps4NewVmOutage", commandCapture.getValue().commands.get(0).command);
    }

    @Test
    public void clearOutage() throws PanoptaServiceException {
        resource.clearVmOutage(vmId, outageId);
        verify(commandService).executeCommand(commandCapture.capture());
        assertEquals("Vps4ClearVmOutage", commandCapture.getValue().commands.get(0).command);
    }
}
