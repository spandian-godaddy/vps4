package com.godaddy.vps4.sso.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Vps4SsoToken {
    private static final int SUCCESS = 1;

    private final int code;
    private final String message;
    private final String data;

    @JsonCreator
    public Vps4SsoToken(@JsonProperty("code") int code,
                        @JsonProperty("message") String message,
                        @JsonProperty("data") String data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public String value() {
        if (code != SUCCESS) {
            throw new RuntimeException("SSO API returned invalid code " + code + ". Message: " + message);
        }
        return data;
    }
}
