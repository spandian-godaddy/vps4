package com.godaddy.vps4.web.security.sso;

public class VerificationException extends Exception {

    private static final long serialVersionUID = 1L;

    public VerificationException(String message) {
        super(message);
    }

    public VerificationException(String message, Throwable e) {
        super(message, e);
    }

}
