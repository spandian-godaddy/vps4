package com.godaddy.vps4.util;

public class PollerTimedOutException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public PollerTimedOutException(String message) {
        super(message);
    }

    public PollerTimedOutException(String message, Throwable e) {
        super(message, e);
    }

}
