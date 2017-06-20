package com.godaddy.vps4.web;

public class Vps4NoShopperException extends Vps4Exception {

    private static final long serialVersionUID = 2L;

    public Vps4NoShopperException() {
        super("SHOPPER_ID_REQUIRED", "Shopper-ID required, cannot be null");
    }

}
