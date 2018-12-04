package com.godaddy.vps4.console;

public class CouldNotRetrieveConsoleException extends RuntimeException {

    private static final long serialVersionUID = -5266197944659183646L;

    public CouldNotRetrieveConsoleException(String message) {
        super(message);
    }
}
