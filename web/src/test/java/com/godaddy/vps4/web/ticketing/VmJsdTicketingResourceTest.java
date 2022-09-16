package com.godaddy.vps4.web.ticketing;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jsd.JsdService;
import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;
import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmJsdTicketingResourceTest {
    private VmResource vmResource = mock(VmResource.class);
    private CreditService creditService = mock(CreditService.class);
    private JsdService jsdService = mock(JsdService.class);

    private VmJsdTicketingResource resource = new VmJsdTicketingResource(vmResource, jsdService, creditService);
    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();

    private VirtualMachine vm = createMockVm();

    private VirtualMachineCredit selfManagedCredit = createMockCredit(20,0);
    private VirtualMachineCredit vmCredit = createMockCredit(20,2);
    private VirtualMachineCredit dedCredit = createMockCredit(60, 2);
    private ArgumentCaptor<CreateJsdTicketRequest> argument = ArgumentCaptor.forClass(CreateJsdTicketRequest.class);
    private VmJsdTicketingResource.CreateTicketRequest request = new VmJsdTicketingResource.CreateTicketRequest();

    private VirtualMachine createMockVm() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.vmId = vmId;
        vm.orionGuid = orionGuid;
        IpAddress ip = new IpAddress();
        ip.ipAddress = "10.0.0.1";
        vm.primaryIpAddress = ip;
        DataCenter dc = new DataCenter();
        dc.dataCenterId = 2;
        vm.dataCenter = dc;
        return vm;
    }


    private VirtualMachineCredit createMockCredit(int tier, int managedLevel) {
        Map<String, String> planFeatures = new HashMap<>();

        planFeatures.put(ECommCreditService.PlanFeatures.TIER.toString(), String.valueOf(tier));
        planFeatures.put(ECommCreditService.PlanFeatures.MANAGED_LEVEL.toString(),  String.valueOf(managedLevel));
        return new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withPlanFeatures(planFeatures)
                .withShopperID("shopper")
                .withResellerID("123456")
                .build();
    }

    @Before
    public void setUp() {
        vm.orionGuid = orionGuid;
        request.shopperId = "fake-shopper";
        request.summary = "Monitoring Event - Agent Heartbeat (0000000)";
        request.partnerCustomerKey = "partnerCustomerKey";
        request.severity = "standard";
        request.outageId = "1231231";
        request.metricTypes = "internal.agent.heartbeat";
        request.metricInfo = "Agent Heartbeat";
        request.metricReasons = "Agent Heartbeat (10.0.0.1)";

        when(vmResource.getVm(vmId)).thenReturn(vm);

        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
    }

    @Test
    public void callsCreateTicketService() {
        resource.createTicket(vmId, request);

        verify(jsdService).createTicket(any(CreateJsdTicketRequest.class));
    }

    @Test
    public void passesCorrectParamsFromRequest() {
        resource.createTicket(vmId, request);

        verify(jsdService).createTicket(argument.capture());
        CreateJsdTicketRequest createJsdTicketRequest = argument.getValue();

        assertEquals(vm.orionGuid.toString(), createJsdTicketRequest.orionGuid);
        assertEquals(request.shopperId, createJsdTicketRequest.shopperId);
        assertEquals(request.summary, createJsdTicketRequest.summary);
        assertEquals(request.partnerCustomerKey, createJsdTicketRequest.partnerCustomerKey);
        assertEquals( "123456", createJsdTicketRequest.plid);
        assertEquals(vm.primaryIpAddress.ipAddress, createJsdTicketRequest.fqdn);
        assertEquals(request.severity, createJsdTicketRequest.severity);
        assertEquals(request.outageId, createJsdTicketRequest.outageId);
        assertEquals("https://my.panopta.com/outage/manageIncident?incident_id=" + request.outageId, createJsdTicketRequest.outageIdUrl);
        assertEquals("vps4", createJsdTicketRequest.supportProduct);
        assertEquals("Fully Managed", createJsdTicketRequest.customerProduct);
        assertEquals(request.metricTypes, createJsdTicketRequest.metricTypes);
        assertEquals("a2", createJsdTicketRequest.dataCenter);
    }

    @Test
    public void throwsErrorForSelfManaged() {
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(selfManagedCredit);

        try{
            resource.createTicket(vmId, request);
            fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("NOT_ALLOWED_FOR_SELF_MANAGED", e.getId());
        }
    }

    @Test
    public void passesCorrectParamsForDedicated() {
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(dedCredit);

        resource.createTicket(vmId, request);

        verify(jsdService).createTicket(argument.capture());
        CreateJsdTicketRequest createJsdTicketRequest = argument.getValue();

        assertEquals("ded4", createJsdTicketRequest.supportProduct);
    }

    @Test
    public void callsSearchTicketService() {
        Long outageIdStr = Long.parseLong(request.outageId);
        resource.searchTicket(vmId, outageIdStr);

        verify(jsdService).searchTicket(vm.primaryIpAddress.ipAddress, outageIdStr, vm.orionGuid);
    }

    @Test
    public void callsCommentTicketServiceCorrectly() {
        Instant timestamp = Instant.now();
        VmJsdTicketingResource.CommentTicketRequest req = new VmJsdTicketingResource.CommentTicketRequest();
        req.timestamp = timestamp;
        req.items = request.metricInfo;

        resource.commentTicket(vmId, "ticketId", req);

        verify(jsdService).commentTicket("ticketId",  "10.0.0.1", request.metricInfo, timestamp);
    }
}
