package com.godaddy.vps4.orchestration.monitoring;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.CommandContext;

public class DeletePanoptaCustomerTest {

    PanoptaService panoptaService = mock(PanoptaService.class);
    CommandContext context = mock(CommandContext.class);
    UUID vmId = UUID.randomUUID();

    DeletePanoptaCustomer command = new DeletePanoptaCustomer(panoptaService);

    @Test
    public void callsPanoptaDeleteCustomer() {
        assertNull(command.execute(context, vmId));
        verify(panoptaService).deleteCustomer(vmId);
    }

}
