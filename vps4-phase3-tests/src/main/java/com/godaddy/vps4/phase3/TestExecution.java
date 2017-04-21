package com.godaddy.vps4.phase3;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TestExecution {

    final VmTest test;

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
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "TestExecution [test=" + test + ", exception=" + exception + ", status=" + status + "]";
    }


}
