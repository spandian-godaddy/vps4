package com.godaddy.vps4.orchestration.panopta;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class CreatePanoptaCustomerTest {

    private PanoptaService panoptaServiceMock;
    private CreatePanoptaCustomer command;
    private CreatePanoptaCustomer.Request request;
    private CommandContext contextMock;
    private PanoptaCustomer panoptaCustomerMock;
    private UUID fakeVmId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        panoptaServiceMock = mock(PanoptaService.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        contextMock = mock(CommandContext.class);

        command = new CreatePanoptaCustomer(panoptaServiceMock);
        setupMockContext();
        setupCommandRequest();
    }

    private void setupMockContext() {
        when(contextMock.execute(eq("CreatePanoptaCustomer"), any(Function.class), eq(PanoptaCustomer.class)))
                .thenReturn(panoptaCustomerMock);
    }

    private void setupCommandRequest() {
        request = new CreatePanoptaCustomer.Request();
        request.vmId = fakeVmId;
    }

    @Test
    public void invokesCreatePanoptaCustomerRequest() {
        command.execute(contextMock, request);

        verify(contextMock, times(1)).execute(eq("CreatePanoptaCustomer"), any(Function.class),
                                              eq(PanoptaCustomer.class));
    }
}