package com.godaddy.vps4.orchestration;

public class NoRetryException extends RuntimeException {
    private static final long serialVersionUID = 20L;

    public NoRetryException(String message, Throwable e) {
        super(message, e);
    }
}
