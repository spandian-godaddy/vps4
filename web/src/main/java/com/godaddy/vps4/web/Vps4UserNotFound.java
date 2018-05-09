package com.godaddy.vps4.web;

public class Vps4UserNotFound extends Exception {

    private static final long serialVersionUID = 1L;

    public Vps4UserNotFound(String message) {
        super(message);
    }

    public Vps4UserNotFound(String message, Throwable e) {
        super(message, e);
    }
}
