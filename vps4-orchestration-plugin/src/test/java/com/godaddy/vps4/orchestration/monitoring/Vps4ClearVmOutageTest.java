package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4ClearVmOutageTest {

    private ActionService actionService = mock(ActionService.class);
    private CreditService creditService = mock(CreditService.class);
    private CommandContext context = mock(CommandContext.class);
    private Config config = mock(Config.class);
    private Vps4ClearVmOutage vps4ClearVmOutage;
    Vps4ClearVmOutage.Request request;
    VirtualMachineCredit credit;
    VmOutage outage;

    @Before
    public void setup() {
        vps4ClearVmOutage = new Vps4ClearVmOutage(actionService, creditService, config);

        when(config.get("jsd.enabled", "false")).thenReturn("true");

        request = new Vps4ClearVmOutage.Request();
        request.actionId = 123321;
        request.outageId = 321123;
        request.virtualMachine = mock(VirtualMachine.class);
        request.virtualMachine.name="TestVm";
        request.virtualMachine.primaryIpAddress = new IpAddress();
        request.virtualMachine.primaryIpAddress.ipAddress = "192.168.1.3";
        request.virtualMachine.orionGuid = UUID.randomUUID();
        request.virtualMachine.vmId = UUID.randomUUID();
        when(request.virtualMachine.isActive()).thenReturn(true);

        credit = mock(VirtualMachineCredit.class);
        when(credit.getOrionGuid()).thenReturn(request.virtualMachine.orionGuid);
        when(credit.getShopperId()).thenReturn("testShopperId");
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        when(credit.isAccountActive()).thenReturn(true);
        when(credit.isManaged()).thenReturn(false);

        outage = new VmOutage();
        outage.panoptaOutageId = request.outageId;
        outage.domainMonitoringMetadata = Collections.emptyList();
        outage.metrics = new HashSet<>();
        outage.metrics.add(VmMetric.CPU);
        outage.ended = Instant.now();
        when(context.execute(eq("GetPanoptaOutage"), eq(GetPanoptaOutage.class), any())).thenReturn(outage);
    }

    @Test
    public void testExecuteWithAction() {
        ArgumentCaptor<GetPanoptaOutage.Request> getPanoptaOutageRequestCaptor = ArgumentCaptor.forClass(GetPanoptaOutage.Request.class);
        ArgumentCaptor<VmOutageEmailRequest> vmOutageEmailRequestArgumentCaptor = ArgumentCaptor.forClass(VmOutageEmailRequest.class);

        vps4ClearVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("GetPanoptaOutage"), eq(GetPanoptaOutage.class), getPanoptaOutageRequestCaptor.capture());
        GetPanoptaOutage.Request actualRequest = getPanoptaOutageRequestCaptor.getValue();
        Assert.assertEquals(request.virtualMachine.vmId, actualRequest.vmId);
        Assert.assertEquals(request.outageId, actualRequest.outageId);
        verify(context).execute(eq("SendOutageClearNotificationEmail"), eq(SendVmOutageResolvedEmail.class),
                vmOutageEmailRequestArgumentCaptor.capture());
        VmOutageEmailRequest arg2 = vmOutageEmailRequestArgumentCaptor.getValue();
        Assert.assertEquals(request.virtualMachine.name, arg2.accountName);
        Assert.assertEquals(request.virtualMachine.primaryIpAddress.ipAddress, arg2.ipAddress);
        Assert.assertEquals(credit.getOrionGuid(), arg2.orionGuid);
        Assert.assertEquals(credit.getShopperId(), arg2.shopperId);
        Assert.assertEquals(request.virtualMachine.vmId, arg2.vmId);
        Assert.assertEquals(outage.panoptaOutageId, arg2.vmOutage.panoptaOutageId);
    }


    @Test
    public void executeEmailCommandWhenCreditIsFullyManaged() {
        when(credit.isManaged()).thenReturn(true);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context).execute(eq("GetPanoptaOutage"), eq(GetPanoptaOutage.class), any());
        verify(context).execute(eq("SendOutageClearNotificationEmail"), eq(SendVmOutageResolvedEmail.class), any());
    }

    @Test
    public void noEmailCommandWhenAccountNotActive() {
        when(credit.isAccountActive()).thenReturn(false);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("SendOutageClearNotificationEmail"), eq(SendVmOutageResolvedEmail.class), any());
    }

    @Test
    public void noEmailCommandWhenCreditNotFound() {
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("SendOutageClearNotificationEmail"), eq(SendVmOutageResolvedEmail.class), any());
    }

    @Test
    public void noEmailCommandWhenVmDestroyed() {
        when(request.virtualMachine.isActive()).thenReturn(false);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("SendOutageClearNotificationEmail"), eq(SendVmOutageResolvedEmail.class), any());
    }

    @Test
    public void createJsdCommandWhenCreditIsFullyManaged() {
        ArgumentCaptor<ClearJsdOutageTicket.Request> clearJsdOutageTicketArgumentCaptor = ArgumentCaptor.forClass(ClearJsdOutageTicket.Request.class);

        when(credit.isManaged()).thenReturn(true);
        vps4ClearVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), clearJsdOutageTicketArgumentCaptor.capture());
        ClearJsdOutageTicket.Request actualRequest = clearJsdOutageTicketArgumentCaptor.getValue();
        Assert.assertEquals(request.virtualMachine.vmId, actualRequest.vmId);
        Assert.assertEquals(request.outageId, actualRequest.outageId);
        Assert.assertEquals("CPU", actualRequest.outageMetrics);
        Assert.assertEquals(outage.ended, actualRequest.outageTimestamp);
    }

    @Test
    public void createJsdCommandCorrectlyOnMultipleMetrics() {
        ArgumentCaptor<ClearJsdOutageTicket.Request> clearJsdOutageTicketArgumentCaptor = ArgumentCaptor.forClass(ClearJsdOutageTicket.Request.class);
        outage.metrics.add(VmMetric.HTTPS);
        outage.metrics.add(VmMetric.HTTP);
        VmOutage.DomainMonitoringMetadata dmm1 = new VmOutage.DomainMonitoringMetadata();
        dmm1.additionalFqdn = "testDomain.here";
        dmm1.metric = VmMetric.HTTPS;
        VmOutage.DomainMonitoringMetadata dmm2 = new VmOutage.DomainMonitoringMetadata();
        dmm2.additionalFqdn = "testDomain2.here";
        dmm2.metric = VmMetric.HTTP;

        outage.domainMonitoringMetadata = Arrays.asList(dmm1, dmm2);
        when(credit.isManaged()).thenReturn(true);
        vps4ClearVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), clearJsdOutageTicketArgumentCaptor.capture());
        ClearJsdOutageTicket.Request actualRequest = clearJsdOutageTicketArgumentCaptor.getValue();
        Assert.assertEquals(request.virtualMachine.vmId, actualRequest.vmId);
        Assert.assertEquals(request.outageId, actualRequest.outageId);
        Assert.assertEquals("CPU, HTTPS (testDomain.here), HTTP (testDomain2.here)", actualRequest.outageMetrics);
        Assert.assertEquals(outage.ended, actualRequest.outageTimestamp);
    }

    @Test
    public void noJsdCommandWhenCreditIsSelfManaged() {
        when(credit.isManaged()).thenReturn(false);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenAccountNotActive() {
        when(credit.isAccountActive()).thenReturn(false);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenCreditNotFound() {
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenVmDestroyed() {
        when(request.virtualMachine.isActive()).thenReturn(false);
        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenConfigReturnsFalse() {
        when(config.get("jsd.enabled", "false")).thenReturn("false");
        when(credit.isManaged()).thenReturn(true);

        vps4ClearVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("ClearJsdOutageTicket"), eq(ClearJsdOutageTicket.class), any());
    }
}
