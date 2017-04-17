package com.godaddy.vps4.handler;

public class Vps4MessageHandlerException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public Vps4MessageHandlerException(String errorMessage) {
        super(errorMessage);
    }
    
    public Vps4MessageHandlerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }
}
