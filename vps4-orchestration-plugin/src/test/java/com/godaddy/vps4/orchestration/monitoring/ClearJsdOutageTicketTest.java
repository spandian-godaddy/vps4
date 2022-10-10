package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.jsd.JsdService;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClearJsdOutageTicketTest {
    @Mock private CommandContext context;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private JsdService jsdService;

    private ClearJsdOutageTicket clearJsdOutageTicket;
    private ClearJsdOutageTicket.Request request;

    private final long outageId = 321123;
    private final UUID orionGuid = UUID.randomUUID();
    private final UUID vmId = UUID.randomUUID();
    private final Instant timestamp = Instant.now();
    private JsdCreatedIssue issue = new JsdCreatedIssue();
    private JsdIssueSearchResult result = new JsdIssueSearchResult();
    private VirtualMachine vm;

    @Before
    public void setUp() throws Exception {
        createMockVm();

        issue.issueKey = "TEST-TICKET-123";
        result.issues = Collections.singletonList(issue);

        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        when(jsdService.searchTicket(vm.primaryIpAddress.ipAddress, outageId, orionGuid)).thenReturn(result);

        clearJsdOutageTicket = new ClearJsdOutageTicket(virtualMachineService, jsdService);
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

    private ClearJsdOutageTicket.Request setupRequest() {
        ClearJsdOutageTicket.Request request = new ClearJsdOutageTicket.Request();
        request.vmId = vmId;
        request.outageId = outageId;
        request.outageMetrics = "[CPU]";
        request.outageTimestamp = timestamp;
        return request;
    }

    @Test
    public void callsSearchTicketService() {
        clearJsdOutageTicket.execute(context, request);

        verify(jsdService).searchTicket(vm.primaryIpAddress.ipAddress, outageId, orionGuid);
    }

    @Test
    public void callsUpdateTicketService() {
        clearJsdOutageTicket.execute(context, request);

        verify(jsdService).commentTicket(issue.issueKey, vm.primaryIpAddress.ipAddress, request.outageMetrics, request.outageTimestamp);
    }

    @Test
    public void doesNotUpdateIfTicketServiceReturnsEmptyList() {
        result.issues = Collections.emptyList();
        verify(jsdService, never()).commentTicket(anyString(), anyString(), anyString(), any(Instant.class));
    }

    @Test
    public void doesNotUpdateIfTicketServiceReturnsNullList() {
        result.issues = null;
        verify(jsdService, never()).commentTicket(anyString(), anyString(), anyString(), any(Instant.class));
    }
    @Test
    public void doesNotUpdateIfTicketServiceReturnsNull() {
        result = null;
        verify(jsdService, never()).commentTicket(anyString(), anyString(), anyString(), any(Instant.class));
    }
}