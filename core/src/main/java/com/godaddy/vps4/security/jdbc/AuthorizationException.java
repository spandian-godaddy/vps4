package com.godaddy.vps4.security.jdbc;

public class AuthorizationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable e) {
        super(message, e);
    }

}
