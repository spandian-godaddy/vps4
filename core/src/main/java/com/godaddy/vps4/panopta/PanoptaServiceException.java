package com.godaddy.vps4.panopta;

public class PanoptaServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String id;

    public PanoptaServiceException(String id, String message) {
        super(message);
        this.id = id;
    }

    public PanoptaServiceException(String id, String message, Throwable e) {
        super(message, e);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
