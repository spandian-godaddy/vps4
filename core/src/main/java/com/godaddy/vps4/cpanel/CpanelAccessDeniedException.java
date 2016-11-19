package com.godaddy.vps4.cpanel;

public class CpanelAccessDeniedException extends Exception {

    private static final long serialVersionUID = 1L;

    public CpanelAccessDeniedException(String message) {
        super(message);
    }

    public CpanelAccessDeniedException(String message, Throwable e) {
        super(message, e);
    }
}
