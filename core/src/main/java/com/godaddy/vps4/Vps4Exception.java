package com.godaddy.vps4;

public class Vps4Exception extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String id;

    public Vps4Exception(String id, String message) {
        super(message);
        this.id = id;
    }

    public Vps4Exception(String id, String message, Throwable cause) {
        super(message, cause);
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
