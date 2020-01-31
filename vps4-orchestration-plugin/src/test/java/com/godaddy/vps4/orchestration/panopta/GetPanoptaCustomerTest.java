package com.godaddy.vps4.orchestration.panopta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class GetPanoptaCustomerTest {

    private PanoptaService panoptaService = mock(PanoptaService.class);
    private String shopperId = "fake-shopper-id";
    private Injector injector = Guice.createInjector();

    private GetPanoptaCustomer command = new GetPanoptaCustomer(panoptaService);
    private CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void callsPanoptaServiceGetCustomer() {
        command.execute(context, shopperId);
        verify(panoptaService).getCustomer(shopperId);
    }

    @Test
    public void returnsPanoptaCustomer() {
        PanoptaCustomer panoptaCustomer = new PanoptaCustomer("customer-key", "partner-customer-key");
        when(panoptaService.getCustomer(shopperId)).thenReturn(panoptaCustomer);

        PanoptaCustomer response = command.execute(context, shopperId);
        assertEquals(panoptaCustomer, response);
    }

}