package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

import javax.ws.rs.core.UriInfo;

public class VmOutageResourceTest {

    private final VmResource vmResource = mock(VmResource.class);
    private final CommandService commandService = mock(CommandService.class);
    private final PanoptaService panoptaService = mock(PanoptaService.class);
    private final VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private final ActionService actionService = mock(ActionService.class);
    private final GDUser gdUser = mock(GDUser.class);

    private final VmOutageResource resource = new VmOutageResource(vmResource, commandService, panoptaService, actionService,
            gdUser);
    private final VmOutageRequest request = new VmOutageRequest("2021-12-10T03:00:38Z");
    private final UUID vmId = UUID.randomUUID();
    private VirtualMachine vm;
    private final int outageId = 23;
    long actionId = 123321;
    private UriInfo uriInfo;

    private final ArgumentCaptor<CommandGroupSpec> commandCapture = ArgumentCaptor.forClass(CommandGroupSpec.class);

    @Before
    public void setUp() throws PanoptaServiceException {
        String shopperId = "fake-shopper-id";

        vm = new VirtualMachine();
        vm.hostname = "TestHostname";
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.primaryIpAddress = new IpAddress();
        vm.primaryIpAddress.ipAddress = "127.0.0.1";
        vm.validUntil = Instant.MAX;
        vm.canceled = Instant.MAX;
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);

        when(credit.isManaged()).thenReturn(false);
        when(credit.isAccountActive()).thenReturn(true);

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

        Set<String> panoptaMetrics = new HashSet<>(Arrays.asList("DISK", "RAM"));
        when(panoptaService.getOutageMetrics(vmId)).thenReturn(panoptaMetrics);
        setupUri();
    }

    private void setupUri(){
        uriInfo = mock(UriInfo.class);
        URI uri = null;
        try {
            uri = new URI("http://fakeUri/something/something/");
        } catch (URISyntaxException e) {
            // do nothing
        }
        when(uriInfo.getAbsolutePath()).thenReturn(uri);
    }

    @Test
    public void getOutageList() throws PanoptaServiceException {
        resource.getVmOutageList(vmId, null, null, null, 10, 0, uriInfo);
        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutages(vmId, null, null, null);
    }

    @Test
    public void getActiveFilteredOutageList() throws PanoptaServiceException {
        resource.getVmOutageList(vmId, null, null, VmOutage.Status.ACTIVE, 10, 0, uriInfo);
        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutages(vmId, null, null, VmOutage.Status.ACTIVE);
    }

    @Test
    public void getDaysAgoFilteredOutageList() throws PanoptaServiceException {
        resource.getVmOutageList(vmId, 1, null, null, 10, 0, uriInfo);
        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutages(vmId, 1, null, null);
    }

    @Test
    public void getMetricFilteredOutageList() throws PanoptaServiceException {
        resource.getVmOutageList(vmId, null, VmMetric.CPU, null, 10, 0, uriInfo);
        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutages(vmId, null, VmMetric.CPU, null);
    }

    @Test
    public void createOutage() {
        resource.newVmOutage(vmId, outageId);
        verify(commandService).executeCommand(commandCapture.capture());
        assertEquals("Vps4NewVmOutage", commandCapture.getValue().commands.get(0).command);
    }

    @Test
    public void clearOutage() {
        resource.clearVmOutage(vmId, outageId, request);
        verify(commandService).executeCommand(commandCapture.capture());
        assertEquals("Vps4ClearVmOutage", commandCapture.getValue().commands.get(0).command);
    }

    @Test
    public void createOutageInactiveServer() {
        vm.canceled = Instant.now().minus(1, ChronoUnit.DAYS);
        resource.newVmOutage(vmId, outageId);
        verify(commandService, never()).executeCommand(commandCapture.capture());
    }

    @Test
    public void clearOutageInactiveService() {
        vm.canceled = Instant.now().minus(1, ChronoUnit.DAYS);
        resource.clearVmOutage(vmId, outageId, request);
        verify(commandService, never()).executeCommand(commandCapture.capture());
    }

    @Test
    public void getOutageMetrics() throws PanoptaServiceException {
        Set<String> expected = new HashSet<>(Arrays.asList("DISK", "RAM"));

        Set<String> actual = resource.getOutageMetrics(vmId);

        verify(vmResource).getVm(vmId);
        verify(panoptaService).getOutageMetrics(vmId);
        assertEquals(expected, actual);
    }
}
