package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.hfs.config.Config;

import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4SyncVmStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerSpec;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4NewVmOutageTest {

    private ActionService actionService = mock(ActionService.class);
    private CreditService creditService = mock(CreditService.class);
    private VmService vmService = mock(VmService.class);
    private CommandContext context = mock(CommandContext.class);
    private Config config = mock(Config.class);

    private Vps4NewVmOutage vps4NewVmOutage;
    private VmExtendedInfo vmExtendedInfo;
    Vps4NewVmOutage.Request request;
    VirtualMachineCredit credit;
    VmOutage outage;

    @Before
    public void setup() {
        vps4NewVmOutage = new Vps4NewVmOutage(actionService, creditService, vmService, config);

        when(config.get("jsd.enabled", "false")).thenReturn("true");

        request = new Vps4NewVmOutage.Request();
        request.actionId = 123321;
        request.outageId = 321123;
        request.virtualMachine = mock(VirtualMachine.class);
        request.virtualMachine.name="TestVm";
        request.virtualMachine.primaryIpAddress = new IpAddress();
        request.virtualMachine.primaryIpAddress.ipAddress = "192.168.1.3";
        request.virtualMachine.orionGuid = UUID.randomUUID();
        request.virtualMachine.vmId = UUID.randomUUID();
        request.partnerCustomerKey = "testKey_testShopperId";
        request.virtualMachine.spec = new ServerSpec();
        request.virtualMachine.spec.serverType = new ServerType();
        request.virtualMachine.spec.serverType.serverType = ServerType.Type.VIRTUAL;
        when(request.virtualMachine.isActive()).thenReturn(true);

        credit = mock(VirtualMachineCredit.class);
        when(credit.getEntitlementId()).thenReturn(request.virtualMachine.orionGuid);
        when(credit.getShopperId()).thenReturn("testShopperId");
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        when(credit.isAccountActive()).thenReturn(true);
        when(credit.isManaged()).thenReturn(false);

        outage = new VmOutage();
        outage.panoptaOutageId = request.outageId;
        outage.metrics = new HashSet<>();
        outage.metrics.add(VmMetric.CPU);
        outage.severity = "standard";
        outage.reason = "oopsie whoopsie";
        outage.domainMonitoringMetadata = Collections.emptyList();
        when(context.execute(eq("GetPanoptaOutage"), eq(GetPanoptaOutage.class), any())).thenReturn(outage);

        vmExtendedInfo = new VmExtendedInfo();
        vmExtendedInfo.extended = new Extended();
        vmExtendedInfo.extended.hypervisorHostname = "phx3plohvmn0350";
        when(vmService.getVmExtendedInfo(request.virtualMachine.hfsVmId)).thenReturn(vmExtendedInfo);
    }

    @Test
    public void testExecuteWithAction() {
        ArgumentCaptor<GetPanoptaOutage.Request> getPanoptaOutageRequestCaptor = ArgumentCaptor.forClass(GetPanoptaOutage.Request.class);
        ArgumentCaptor<VmOutageEmailRequest> vmOutageEmailRequestArgumentCaptor = ArgumentCaptor.forClass(VmOutageEmailRequest.class);

        vps4NewVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("GetPanoptaOutage"), eq(GetPanoptaOutage.class), getPanoptaOutageRequestCaptor.capture());
        GetPanoptaOutage.Request actualRequest = getPanoptaOutageRequestCaptor.getValue();
        Assert.assertEquals(request.virtualMachine.vmId, actualRequest.vmId);
        Assert.assertEquals(request.outageId, actualRequest.outageId);
        verify(context).execute(eq("SendOutageNotificationEmail"), eq(SendVmOutageCreatedEmail.class),
                vmOutageEmailRequestArgumentCaptor.capture());
        VmOutageEmailRequest arg2 = vmOutageEmailRequestArgumentCaptor.getValue();
        Assert.assertEquals(request.virtualMachine.name, arg2.accountName);
        Assert.assertEquals(request.virtualMachine.primaryIpAddress.ipAddress, arg2.ipAddress);
        Assert.assertEquals(credit.getEntitlementId(), arg2.orionGuid);
        Assert.assertEquals(credit.getShopperId(), arg2.shopperId);
        Assert.assertEquals(request.virtualMachine.vmId, arg2.vmId);
        Assert.assertEquals(outage.panoptaOutageId, arg2.vmOutage.panoptaOutageId);
    }

    @Test
    public void noEmailCommandWhenAccountNotActive() {
        when(credit.isAccountActive()).thenReturn(false);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("SendOutageNotificationEmail"), eq(SendVmOutageCreatedEmail.class), any());
    }

    @Test
    public void noEmailCommandWhenCreditNotFound() {
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("SendOutageNotificationEmail"), eq(SendVmOutageCreatedEmail.class), any());
    }

    @Test
    public void noEmailCommandWhenVmDestroyed() {
        when(request.virtualMachine.isActive()).thenReturn(false);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("SendOutageNotificationEmail"), eq(SendVmOutageCreatedEmail.class), any());
    }

    @Test
    public void executeEmailCommandWhenCreditIsFullyManaged() {
        when(credit.isManaged()).thenReturn(true);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context).execute(eq("SendOutageNotificationEmail"), eq(SendVmOutageCreatedEmail.class), any());
    }

    @Test
    public void executeJsdCommandWhenCreditIsFullyManaged() {
        ArgumentCaptor<CreateJsdOutageTicket.Request> createJsdOutageTicketArgumentCaptor = ArgumentCaptor.forClass(CreateJsdOutageTicket.Request.class);

        when(credit.isManaged()).thenReturn(true);
        vps4NewVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), createJsdOutageTicketArgumentCaptor.capture());
        CreateJsdOutageTicket.Request arg2 = createJsdOutageTicketArgumentCaptor.getValue();
        Assert.assertEquals(outage.panoptaOutageId, Long.parseLong(arg2.outageId));
        Assert.assertEquals(request.virtualMachine.vmId, arg2.vmId);
        Assert.assertEquals("testShopperId", arg2.shopperId);
        Assert.assertEquals(request.partnerCustomerKey, arg2.partnerCustomerKey);
        Assert.assertEquals("CPU", arg2.metricInfo);
        Assert.assertEquals("CPU", arg2.metricTypes);
        Assert.assertEquals(outage.reason, arg2.metricReasons);
        Assert.assertEquals("Monitoring Event - " + outage.metrics.toString() + " - "+ arg2.metricReasons + " (" + 321123 + ")", arg2.summary);
        Assert.assertEquals(vmExtendedInfo.extended.hypervisorHostname, arg2.hypervisorHostname);
    }

    @Test
    public void hypervisorHostnameIsNullIfDedicated() {
        when(credit.isManaged()).thenReturn(true);
        request.virtualMachine.spec.serverType.serverType = ServerType.Type.DEDICATED;
        ArgumentCaptor<CreateJsdOutageTicket.Request> createJsdOutageTicketArgumentCaptor = ArgumentCaptor.forClass(CreateJsdOutageTicket.Request.class);

        vps4NewVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), createJsdOutageTicketArgumentCaptor.capture());
        CreateJsdOutageTicket.Request arg2 = createJsdOutageTicketArgumentCaptor.getValue();
        Assert.assertEquals(null, arg2.hypervisorHostname);
    }

    @Test
    public void hypervisorHostnameIsNullIfVmExtendedInfoIsNull() {
        when(vmService.getVmExtendedInfo(request.virtualMachine.hfsVmId)).thenReturn(null);
        when(credit.isManaged()).thenReturn(true);
        ArgumentCaptor<CreateJsdOutageTicket.Request> createJsdOutageTicketArgumentCaptor = ArgumentCaptor.forClass(CreateJsdOutageTicket.Request.class);

        vps4NewVmOutage.executeWithAction(context, request);

        verify(context).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), createJsdOutageTicketArgumentCaptor.capture());
        CreateJsdOutageTicket.Request arg2 = createJsdOutageTicketArgumentCaptor.getValue();
        Assert.assertEquals(null, arg2.hypervisorHostname);
    }

    @Test
    public void noJsdCommandWhenCreditIsSelfManaged() {
        when(credit.isManaged()).thenReturn(false);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenAccountNotActive() {
        when(credit.isAccountActive()).thenReturn(false);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenCreditNotFound() {
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenVmDestroyed() {
        when(request.virtualMachine.isActive()).thenReturn(false);
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), any());
    }

    @Test
    public void noJsdCommandWhenConfigReturnsFalse() {
        when(config.get("jsd.enabled", "false")).thenReturn("false");
        when(credit.isManaged()).thenReturn(true);

        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, never()).execute(eq("CreateJsdOutageTicket"), eq(CreateJsdOutageTicket.class), any());
    }

    @Test
    public void syncsVmStatus() {
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, times(1)).execute("Vps4SyncVmStatus", Vps4SyncVmStatus.class, request);
    }

    @Test
    public void doesNotSyncDedicatedServerStatus() {
        request.virtualMachine.spec.serverType.serverType = ServerType.Type.DEDICATED;
        vps4NewVmOutage.executeWithAction(context, request);
        verify(context, times(0)).execute("Vps4SyncVmStatus", Vps4SyncVmStatus.class, request);
    }
}
