package com.godaddy.vps4.cpanel;

public class CpanelTimeoutException extends Exception {

    private static final long serialVersionUID = 1L;

    public CpanelTimeoutException(String message) {
        super(message);
    }

    public CpanelTimeoutException(String message, Throwable e) {
        super(message, e);
    }
}
