package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Preference {

    public String currency;
    public String marketId;
    public String emailFormat;

    @Override
    public String toString() {
        return "Preference [currency: " + currency + " marketId: " + marketId +
                " emailFormat: " + emailFormat + "]";
    }
}
