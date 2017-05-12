package com.godaddy.vps4.orchestration.hfs.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.monitoring.DeleteCheck;
import com.godaddy.vps4.orchestration.hfs.monitoring.WaitForPingCheckAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.nodeping.NodePingAction;
import gdg.hfs.vhfs.nodeping.NodePingAction.Status;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class DeleteCheckTest {

    NodePingService nodePingService;
    DeleteCheck command;
    CommandContext context;
    Injector injector;

    @Before
    public void setup() {
        nodePingService = mock(NodePingService.class);
        command = new DeleteCheck(nodePingService);
        injector = Guice.createInjector(binder -> {
            binder.bind(DeleteCheck.class);
            binder.bind(WaitForPingCheckAction.class);
            binder.bind(NodePingService.class).toInstance(nodePingService);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    @Test
    public void testDeleteCheckSuccess() {
        DeleteCheck.Request request = new DeleteCheck.Request(123L, 345L);

        NodePingAction nodePingAction = new NodePingAction();
        nodePingAction.actionId = 234;
        nodePingAction.status = Status.COMPLETE;
        nodePingAction.accountId = request.accountId;
        nodePingAction.checkId = request.checkId;

        when(nodePingService.deleteCheck(request.accountId, request.checkId)).thenReturn(nodePingAction);
        when(nodePingService.getAction(nodePingAction.actionId)).thenReturn(nodePingAction);

        NodePingAction action = command.execute(context, request);

        verify(nodePingService, times(1)).deleteCheck(request.accountId, request.checkId);
        assertEquals(nodePingAction, action);
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteCheckFail() {
        DeleteCheck.Request request = new DeleteCheck.Request(123L, 345L);

        when(nodePingService.deleteCheck(request.accountId, request.checkId))
                .thenThrow(new RuntimeException("Failed to delete check"));

        command.execute(context, request);
    }

}
