package com.godaddy.vps4.security.jdbc;

import com.godaddy.vps4.Vps4Exception;

public class AuthorizationException extends Vps4Exception {

    private static final long serialVersionUID = 1L;

    private static final String ID = "AUTHORIZATION_DENIED";

    public AuthorizationException(String message) {
        super(ID, message);
    }

    public AuthorizationException(String message, Throwable e) {
        super(ID, message, e);
    }

}
