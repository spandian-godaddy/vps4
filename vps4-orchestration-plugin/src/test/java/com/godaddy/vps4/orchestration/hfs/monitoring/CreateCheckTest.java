package com.godaddy.vps4.orchestration.hfs.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.monitoring.CreateCheck;
import com.godaddy.vps4.orchestration.hfs.monitoring.WaitForPingCheckAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.nodeping.NodePingAction;
import gdg.hfs.vhfs.nodeping.NodePingAction.Status;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class CreateCheckTest {

    NodePingService nodePingService;
    CreateCheck command;
    CommandContext context;
    Injector injector;

    @Before
    public void setup() {
        nodePingService = mock(NodePingService.class);
        command = new CreateCheck(nodePingService);
        injector = Guice.createInjector(binder -> {
            binder.bind(CreateCheck.class);
            binder.bind(WaitForPingCheckAction.class);
            binder.bind(NodePingService.class).toInstance(nodePingService);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    @Test
    public void testCreateCheckSuccess() {
        CreateCheck.Request request = new CreateCheck.Request(123L, "192.168.1.1", "TestCheck");

        NodePingAction nodePingAction = new NodePingAction();
        nodePingAction.actionId = 234;
        nodePingAction.status = Status.COMPLETE;
        nodePingAction.accountId = request.accountId;
        nodePingAction.checkId = 345L;
        
        NodePingCheck testCheck = new NodePingCheck(nodePingAction.accountId, nodePingAction.checkId, "NodePingCheckId");

        when(nodePingService.createCheck(request.accountId, request.target, request.label)).thenReturn(nodePingAction);
        when(nodePingService.getAction(nodePingAction.actionId)).thenReturn(nodePingAction);
        when(nodePingService.getCheck(nodePingAction.accountId, nodePingAction.checkId)).thenReturn(testCheck);

        long checkId = command.execute(context, request);

        verify(nodePingService, times(1)).createCheck(request.accountId, request.target, request.label);
        assertEquals(testCheck.checkId, checkId);
    }
    
    @Test(expected = RuntimeException.class)
    public void testCreateCheckFail() {
        CreateCheck.Request request = new CreateCheck.Request(123L, "192.168.1.1", "TestCheck");
        
        when(nodePingService.createCheck(request.accountId, request.target, request.label))
                .thenThrow(new RuntimeException("Failed to create check"));

        command.execute(context, request);
    }
}
