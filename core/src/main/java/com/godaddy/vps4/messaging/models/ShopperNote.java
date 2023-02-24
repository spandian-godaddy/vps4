package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopperNote {

    public String content;
    public String enteredBy;

    @Override
    public String toString() {
        return "ShopperNote [content: " + content + " enteredBy: " + enteredBy + "]";
    }
}
