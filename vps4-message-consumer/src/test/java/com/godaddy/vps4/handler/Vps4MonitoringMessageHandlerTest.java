package com.godaddy.vps4.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;

public class Vps4MonitoringMessageHandlerTest {

    private CommandService commandService = mock(CommandService.class);

    @Before
    public void setupTest() {

    }

    @Test
    public void testHandleMessageDownEventSendsOrchCommand() throws MessageHandlerException {
        ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
        String msgKey = "123";
        String msgValue = "{\"event\": \"down\"}";
        when(record.key()).thenReturn(msgKey);
        when(record.value()).thenReturn(msgValue);

        MessageHandler handler = new Vps4MonitoringMessageHandler(commandService);
        handler.handleMessage(record);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());

        CommandSpec cmdSpec = argument.getValue().commands.get(0);
        assertEquals("HandleMonitoringDownEvent", cmdSpec.command);
        assertEquals(123L, cmdSpec.request);
    }

    @Test
    public void testHandleMessageUpEventDoNothing() throws MessageHandlerException {
        ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
        String msgKey = "123";
        String msgValue = "{\"event\": \"up\"}";
        when(record.key()).thenReturn(msgKey);
        when(record.value()).thenReturn(msgValue);

        MessageHandler handler = new Vps4MonitoringMessageHandler(commandService);
        handler.handleMessage(record);

        verify(commandService, never()).executeCommand(any());
    }

}
