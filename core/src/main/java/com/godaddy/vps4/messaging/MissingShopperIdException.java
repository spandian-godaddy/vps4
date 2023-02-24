package com.godaddy.vps4.messaging;

public class MissingShopperIdException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingShopperIdException(String message) {
        super(message);
    }

    public MissingShopperIdException(String message, Throwable e) {
        super(message, e);
    }

}
