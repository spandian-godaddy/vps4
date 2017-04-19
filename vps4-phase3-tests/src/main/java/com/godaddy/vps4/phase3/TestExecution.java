package com.godaddy.vps4.phase3;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExecution {

    private static final Logger logger = LoggerFactory.getLogger(TestExecution.class);

    final VmTest test;

    volatile Future<?> future;

    volatile Throwable exception;

    volatile TestStatus status = TestStatus.PENDING;

    public TestExecution(VmTest test) {
        this.test = test;
    }

    public void await() throws InterruptedException {
        logger.trace("waiting {}", this.test);
        try {
            future.get();
//            this.status = TestStatus.PASS;
        }
        catch (ExecutionException e) {
            this.exception = exception;
            throw new RuntimeException(e);
//            this.status = TestStatus.FAIL;
        }
    }

    @Override
    public String toString() {
        return "TestExecution [test=" + test + ", exception=" + exception + ", status=" + status + "]";
    }


}
