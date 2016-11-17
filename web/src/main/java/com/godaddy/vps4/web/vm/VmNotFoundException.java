package com.godaddy.vps4.web.vm;

/**
 * Created by abhoite on 11/14/16.
 */
public class VmNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public VmNotFoundException() {
        super();
    }

    public VmNotFoundException(String message) {
        super(message);
    }

    public VmNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public VmNotFoundException(Throwable cause) {
        super(cause);
    }

}
