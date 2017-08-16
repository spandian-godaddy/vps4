package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contact {

    public String nameFirst;
    public String nameLast;

    @Override
    public String toString() {
        return "Contact [nameFirst: " + nameFirst + " nameLast: " + nameLast + "]";
    }
}