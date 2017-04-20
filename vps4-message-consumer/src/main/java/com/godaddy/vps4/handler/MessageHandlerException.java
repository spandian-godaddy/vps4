package com.godaddy.vps4.handler;

public class MessageHandlerException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public MessageHandlerException(String errorMessage) {
        super(errorMessage);
    }
    
    public MessageHandlerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }
}
