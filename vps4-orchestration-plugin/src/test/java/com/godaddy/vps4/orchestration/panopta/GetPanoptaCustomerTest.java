package com.godaddy.vps4.orchestration.panopta;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class GetPanoptaCustomerTest {

    private PanoptaService panoptaServiceMock;
    private GetPanoptaCustomer command;
    private CommandContext contextMock;
    private PanoptaCustomer panoptaCustomerMock;
    private String shopperId = "fake-shopper-id";

    @Before
    public void setUp() throws Exception {
        panoptaServiceMock = mock(PanoptaService.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        contextMock = mock(CommandContext.class);

        command = new GetPanoptaCustomer(panoptaServiceMock);
        setupMockContext();
    }

    private void setupMockContext() {
        when(contextMock.execute(eq("GetPanoptaCustomer"), any(Function.class), eq(PanoptaCustomer.class)))
                .thenReturn(panoptaCustomerMock);
    }

    @Test
    public void invokesGetPanoptaCustomer() {
        command.execute(contextMock, shopperId);

        verify(contextMock, times(1)).execute(eq("GetPanoptaCustomer"), any(Function.class),
                                              eq(PanoptaCustomer.class));
    }

}