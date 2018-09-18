package com.godaddy.vps4.handler;

public class MessageHandlerException extends Exception {
    
    private static final long serialVersionUID = 1L;

    // Give the ability for some exceptions to allow for retry of the message processing
    // Eg: if orch-engine is down or DB is down then we would want to reprocess the message
    // but if the bounceback system is down and we aren't able to send a message then maybe its acceptable to move on.
    private boolean shouldRetry;

    public MessageHandlerException(Throwable e) {
        this(false, e);
    }

    public MessageHandlerException(Boolean shouldRetry, Throwable e) {
        super(e);
        this.shouldRetry = shouldRetry;
    }
    
    public MessageHandlerException(String errorMessage) {
        super(errorMessage);
    }
    
    public MessageHandlerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }

    public boolean shouldRetry() {
        return shouldRetry;
    }
}
