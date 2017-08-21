package com.godaddy.vps4.cpanel;

public class CpanelInvalidUserException extends RuntimeException {

    private static final long serialVersionUID = 10L;

    public CpanelInvalidUserException(String message) {
        super(message);
    }

    public CpanelInvalidUserException(String message, Throwable e) {
        super(message, e);
    }
}
