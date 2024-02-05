package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import org.junit.Test;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.CommandContext;

import static org.mockito.Mockito.*;

public class RemovePanoptaMonitoringTest {

    CreditService creditService = mock(CreditService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    PanoptaService panoptaService = mock(PanoptaService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    String shopperId = "test-shopper";
    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    RemovePanoptaMonitoring.Request request = new RemovePanoptaMonitoring.Request();
    RemovePanoptaMonitoring command = new RemovePanoptaMonitoring(creditService, panoptaDataService, panoptaService);

    @Test
    public void removesMonitoring() throws PanoptaServiceException {
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
        when(credit.getShopperId()).thenReturn(shopperId);
        request.vmId = vmId;
        request.orionGuid = orionGuid;

        command.execute(context, request);

        verify(panoptaService).deleteServer(request.vmId, shopperId);
        verify(panoptaDataService).deleteVirtualMachineAdditionalFqdns(vmId);
        verify(panoptaDataService).setPanoptaServerDestroyed(vmId);
    }
}