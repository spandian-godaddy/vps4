package com.godaddy.vps4.orchestration.monitoring;

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
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateJsdOutageTicketTest {
    @Mock private CommandContext context;
    @Mock private CreditService creditService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private JsdService jsdService;

    private CreateJsdOutageTicket createJsdOutageTicket;
    private CreateJsdOutageTicket.Request request;

    private final String shopperId = "12345";
    private final String outageId = "321123";
    private final UUID orionGuid = UUID.randomUUID();
    private final UUID vmId = UUID.randomUUID();
    private final String partnerCustomerKey = "gdtest_" + shopperId;
    private VirtualMachine vm;
    private VirtualMachineCredit vmCredit = createMockCredit(20,2);
    private VirtualMachineCredit dedCredit = createMockCredit(60, 2);

    private ArgumentCaptor<CreateJsdTicketRequest> argument = ArgumentCaptor.forClass(CreateJsdTicketRequest.class);

    @Before
    public void setUp() throws Exception {
        createMockVm();

        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);

        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);

        createJsdOutageTicket = new CreateJsdOutageTicket(virtualMachineService, jsdService, creditService);
        request = setupRequest();
    }

    private void createMockVm() {
        vm = mock(VirtualMachine.class);
        vm.vmId = vmId;
        vm.orionGuid = orionGuid;
        IpAddress ip = new IpAddress();
        ip.ipAddress = "10.0.0.1";
        vm.primaryIpAddress = ip;
        DataCenter dc = new DataCenter();
        dc.dataCenterId = 2;
        vm.dataCenter = dc;
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

    private CreateJsdOutageTicket.Request setupRequest() {
        String outageSummary = "Monitoring Event - [CPU] - oopsie whoopsie (321123)-  [CPU2] - oopsie whoopsie 2 (321123)";
        CreateJsdOutageTicket.Request request = new CreateJsdOutageTicket.Request();
        request.vmId = vmId;
        request.shopperId = shopperId;
        request.outageId = outageId;
        request.partnerCustomerKey = partnerCustomerKey;
        request.metricInfo = "[CPU]";
        request.metricTypes = "[CPU]";
        request.metricReasons = "oopsie whoopsie";
        request.severity = "standard";
        request.summary = "Monitoring Event - [CPU] - oopsie whoopsie (321123) -" +
                " [CPU2] - oopsie whoopsie 2 (321123) Monitoring Event - [CPU] - oopsie whoopsie (321123) -" +
                " [CPU2] - oopsie whoopsie 2 (321123) Monitoring Event - [CPU] - oopsie whoopsie (321123) - [CPU2] - oopsie whoop";
        request.hypervisorHostname = "phx3plohvmn0350";
        return request;
    }

    @Test
    public void callsCreateTicketService() {
        createJsdOutageTicket.execute(context, request);

        verify(jsdService).createTicket(any(CreateJsdTicketRequest.class));
    }

    @Test
    public void passesCorrectParamsFromRequest() {
        createJsdOutageTicket.execute(context, request);

        verify(jsdService).createTicket(argument.capture());
        CreateJsdTicketRequest createJsdTicketRequest = argument.getValue();

        assertEquals(orionGuid.toString(), createJsdTicketRequest.orionGuid);
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
        assertEquals("phx3plohvmn0350", createJsdTicketRequest.hypervisorHostname);
    }

    @Test
    public void passesCorrectTruncatedSummaryParamsFromRequest() {
        request.summary = request.summary + "extra";
        createJsdOutageTicket.execute(context, request);

        verify(jsdService).createTicket(argument.capture());
        CreateJsdTicketRequest createJsdTicketRequest = argument.getValue();

        assertEquals(request.summary.substring(0,256), createJsdTicketRequest.summary);
    }

    @Test
    public void passesCorrectParamsForDedicated() {
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(dedCredit);

        createJsdOutageTicket.execute(context, request);

        verify(jsdService).createTicket(argument.capture());
        CreateJsdTicketRequest createJsdTicketRequest = argument.getValue();

        assertEquals("ded4", createJsdTicketRequest.supportProduct);
    }
}
