package com.godaddy.vps4.plesk;

public class PleskUrlUnavailableException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public PleskUrlUnavailableException(String message) {
        super(message);
    }

    public PleskUrlUnavailableException(String message, Throwable e) {
        super(message, e);
    }

}
