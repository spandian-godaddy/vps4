package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.vm.VmOutageService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.monitoring.VmOutageResource.VmOutageRequest;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmOutageResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private VmOutageService vmOutageService = mock(VmOutageService.class);
    private CommandService commandService = mock(CommandService.class);
    private CreditService creditService = mock(CreditService.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private GDUser gdUser = mock(GDUser.class);

    private VmOutageResource resource = new VmOutageResource(vmResource, vmOutageService, commandService, creditService);
    private UUID vmId = UUID.randomUUID();
    private VmMetric metric = VmMetric.CPU;
    private VmMetricAlert vmMetricAlert = new VmMetricAlert();
    private VirtualMachine vm;
    private int outageId = 23;
    private String shopperId = "fake-shopper-id";
    private boolean suppressEmail;

    @Before
    public void setUp() {
        suppressEmail = false;

        vm = new VirtualMachine();
        vm.hostname = "TestHostname";
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.primaryIpAddress = new IpAddress();
        vm.primaryIpAddress.ipAddress = "127.0.0.1";
        vm.validUntil = Instant.MAX;
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);

        when(creditService.getVirtualMachineCredit(eq(vm.orionGuid))).thenReturn(credit);
        when(credit.isManaged()).thenReturn(true);
        when(credit.isAccountActive()).thenReturn(true);

        VmOutage vmOutage = new VmOutage();
        vmOutage.metric = VmMetric.CPU;
        when(vmOutageService.getVmOutage(outageId)).thenReturn(vmOutage);
        when(vmOutageService.newVmOutage(eq(vmId), any(VmMetric.class), any(Instant.class),
                                         anyString(), anyLong())).thenReturn(outageId);

        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(gdUser.getShopperId()).thenReturn(shopperId);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
    }

    @Test
    public void getOutageList() {
        resource.getVmOutageList(vmId, null);
        verify(vmResource).getVm(vmId);
        verify(vmOutageService).getVmOutageList(vmId);
    }

    @Test
    public void getMetricFilteredOutageList() {
        resource.getVmOutageList(vmId, metric.name());
        verify(vmResource).getVm(vmId);
        verify(vmOutageService).getVmOutageList(vmId, metric);
    }

    @Test
    public void filterByInvalidMetric() {
        try {
            resource.getVmOutageList(vmId, "UPTIME");
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

    @Test
    public void getOutage() {
        resource.getVmOutage(vmId, outageId);
        verify(vmResource).getVm(vmId);
        verify(vmOutageService).getVmOutage(outageId);
    }

    private VmOutageRequest newOutageRequest() {
        VmOutageRequest request = new VmOutageRequest();
        request.metric = "CPU";
        request.startDate = "2019-11-15 12:40:01 UTC"; //Panopta format
        request.reason = "CPU greater than 90% for more than 1 minute";
        request.panoptaOutageId = -103317320;
        return request;
    }

    @Test
    public void createOutage() {
        VmOutageRequest req = newOutageRequest();
        resource.newVmOutage(vmId, req);
        verify(vmResource).getVm(vmId);
        verify(vmOutageService)
                .newVmOutage(vmId, VmMetric.valueOf(req.metric), Instant.parse("2019-11-15T12:40:01Z"), req.reason,
                             req.panoptaOutageId);
        verify(vmOutageService).getVmOutage(outageId);
    }

    @Test
    public void catchCreateWithInvalidMetric() {
        VmOutageRequest req = newOutageRequest();
        req.metric = "BANDWIDTH";
        try {
            resource.newVmOutage(vmId, req);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

    @Test
    public void catchCreateWithInvalidDateFormat() {
        VmOutageRequest req = newOutageRequest();
        req.startDate = "2019-11-15T12:40:01Z";  // NOT Panopta format
        try {
            resource.newVmOutage(vmId, req);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

    @Test
    public void clearOutage() {
        String endDate = "2019-11-19 13:23:42 UTC"; //Panopta format
        resource.clearVmOutage(vmId, outageId, endDate, suppressEmail);
        verify(vmOutageService).clearVmOutage(outageId, Instant.parse("2019-11-19T13:23:42Z"));
        verify(vmOutageService).getVmOutage(outageId);
    }

    @Test
    public void catchClearWithInvalidDateFormat() {
        String endDate = "2019-11-19T13:23:42Z"; // NOT Panopta format
        try {
            resource.clearVmOutage(vmId, outageId, endDate, suppressEmail);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

    @Test
    public void clearOutageWithoutEndDate() {
        resource.clearVmOutage(vmId, outageId, null, suppressEmail);
        verify(vmOutageService).clearVmOutage(eq(outageId), any(Instant.class));
        verify(vmOutageService).getVmOutage(outageId);
    }

    @Test
    public void invokesSendEmailOnAlerts() {
        VmOutageRequest req = newOutageRequest();
        resource.newVmOutage(vmId, req);
        ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        assertSame(commandGroupSpec.commands.get(0).command, "SendVmOutageEmail");
    }

    @Test
    public void noClearEmailCommandWithSuppressEmailTrue() {
        suppressEmail = true;
        resource.clearVmOutage(vmId, outageId, null, suppressEmail);
        verify(commandService, never()).executeCommand(any());
    }

    @Test
    public void noEmailCommandWhenAccountNotActive() {
        when(credit.isAccountActive()).thenReturn(false);
        VmOutageRequest req = newOutageRequest();
        resource.newVmOutage(vmId, req);
        verify(commandService, never()).executeCommand(any());
    }

    @Test
    public void noEmailCommandWhenCreditNotFound() {
        when(creditService.getVirtualMachineCredit(eq(vm.orionGuid))).thenReturn(null);
        VmOutageRequest req = newOutageRequest();
        resource.newVmOutage(vmId, req);
        verify(commandService, never()).executeCommand(any());
    }

    @Test
    public void noEmailCommandWhenVmDestroyed() {
        vm.validUntil = Instant.now().minus(5, ChronoUnit.MINUTES);
        VmOutageRequest req = newOutageRequest();
        resource.newVmOutage(vmId, req);
        verify(commandService, never()).executeCommand(any());
    }

}
