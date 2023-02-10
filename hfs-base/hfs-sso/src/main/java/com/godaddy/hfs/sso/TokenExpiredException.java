package com.godaddy.hfs.sso;

public class TokenExpiredException extends Exception {

    private static final long serialVersionUID = 1L;

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable e) {
        super(message, e);
    }

}
