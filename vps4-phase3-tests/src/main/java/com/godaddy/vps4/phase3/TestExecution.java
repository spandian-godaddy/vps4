package com.godaddy.vps4.phase3;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExecution {

    final VmTest test;

    private static final Logger logger = LoggerFactory.getLogger(TestExecution.class);

    volatile Future<?> future;

    volatile Throwable exception;

    volatile TestStatus status = TestStatus.PENDING;

    public TestExecution(VmTest test) {
        this.test = test;
    }

    public void await() throws InterruptedException {
        try {
            future.get();
        }
        catch (ExecutionException e) {
            this.exception = exception;
            logger.error("Test aborted: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "TestExecution [status=" + status + " test=" + test + ", exception=" + exception + "]";
    }


}
