package com.godaddy.vps4.scheduler.client;

public class InvalidResponseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidResponseException(String message) {
        super(message);
    }

    public InvalidResponseException(Throwable cause) {
        super(cause);
    }

    public InvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }

}
