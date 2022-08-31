package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VmOutage;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetPanoptaOutageTest {

    PanoptaService panoptaService = mock(PanoptaService.class);
    CommandContext context = mock(CommandContext.class);
    GetPanoptaOutage getPanoptaOutage;

    @Test
    public void TestGetOutage() throws PanoptaServiceException {
        GetPanoptaOutage.Request request = new GetPanoptaOutage.Request();
        getPanoptaOutage = new GetPanoptaOutage(panoptaService);
        when(panoptaService.getOutage(request.vmId, request.outageId)).thenReturn(new VmOutage());

        getPanoptaOutage.execute(context, request);

        verify(panoptaService).getOutage(request.vmId, request.outageId);
    }
}
