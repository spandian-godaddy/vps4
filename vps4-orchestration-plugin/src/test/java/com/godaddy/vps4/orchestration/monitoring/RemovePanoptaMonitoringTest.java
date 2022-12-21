package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.CommandContext;

public class RemovePanoptaMonitoringTest {

    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    PanoptaService panoptaService = mock(PanoptaService.class);
    CommandContext context = mock(CommandContext.class);
    UUID vmId = UUID.randomUUID();

    RemovePanoptaMonitoring command = new RemovePanoptaMonitoring(panoptaDataService, panoptaService);

    @Test
    public void removesMonitoring() {
        command.execute(context, vmId);
        verify(panoptaService).deleteServer(vmId);
        verify(panoptaDataService).deleteVirtualMachineAdditionalFqdns(vmId);
        verify(panoptaDataService).setPanoptaServerDestroyed(vmId);
    }
}