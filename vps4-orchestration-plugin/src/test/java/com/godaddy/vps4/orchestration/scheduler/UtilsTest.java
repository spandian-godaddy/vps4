package com.godaddy.vps4.orchestration.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServerErrorException;

import org.junit.Test;
import org.slf4j.Logger;

import gdg.hfs.orchestration.CommandContext;

public class UtilsTest {

    private CommandContext context = mock(CommandContext .class);
    private Logger logger = mock(Logger.class);
    private Utils.RetryActionHandler handler = mock(Utils.RetryActionHandler.class);
    private ServerErrorException e = new ServerErrorException("test exception", 500);
    private ProcessingException processingException = new ProcessingException("processing exception");


    @Test
    public void testRunWithRetriesForServerErrorExceptionNoRetryNeeded() {
        when(handler.handle()).thenReturn(true);
        Utils.runWithRetriesForServerErrorException(context, logger, handler);
        verify(context, times(1)).sleep(2000);
        verify(handler, times(1)).handle();
    }

    @Test
    public void testRunWithRetriesForServerErrorExceptionLessThenMaxTimes() {
        when(handler.handle()).thenThrow(e).thenThrow(e).thenReturn(true);
        Utils.runWithRetriesForServerErrorException(context, logger, handler);
        verify(context, times(3)).sleep(2000);
        verify(handler, times(3)).handle();
    }

    @Test(expected = ServerErrorException.class)
    public void testRunWithRetriesForServerErrorExceptionMaxTimes() {
        when(handler.handle()).thenThrow(e).thenThrow(e).thenThrow(e).thenThrow(e).thenThrow(e).thenThrow(e);
        Utils.runWithRetriesForServerErrorException(context, logger, handler);
    }

    @Test
    public void testRunWithRetriesForProcessingAndServerExceptionNoRetryNeeded() {
        when(handler.handle()).thenReturn(true);
        Utils.runWithRetriesForServerAndProcessingErrorException(context, logger, handler);
        verify(context, times(1)).sleep(2000);
        verify(handler, times(1)).handle();
    }

    @Test
    public void testRunWithRetriesForProcessingAndServerExceptionLessThenMaxTimes() {
        when(handler.handle()).thenThrow(processingException).thenThrow(e).thenReturn(true);
        Utils.runWithRetriesForServerAndProcessingErrorException(context, logger, handler);
        verify(context, times(3)).sleep(2000);
        verify(handler, times(3)).handle();
    }

    @Test(expected = ServerErrorException.class)
    public void testRetriesForProcessingAndServerExceptionMaxTimesServer() {
        when(handler.handle()).thenThrow(processingException).thenThrow(processingException).thenThrow(e)
                .thenThrow(e).thenThrow(e).thenThrow(e);
        Utils.runWithRetriesForServerAndProcessingErrorException(context, logger, handler);
    }

    @Test(expected = ProcessingException.class)
    public void testRetriesForProcessingAndServerExceptionMaxTimesProcessing() {
        when(handler.handle()).thenThrow(e).thenThrow(processingException).thenThrow(e)
                .thenThrow(e).thenThrow(processingException).thenThrow(e);
        Utils.runWithRetriesForServerAndProcessingErrorException(context, logger, handler);
    }
}
