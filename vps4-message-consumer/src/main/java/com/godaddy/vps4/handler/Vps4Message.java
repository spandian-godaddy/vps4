package com.godaddy.vps4.handler;

import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.json.simple.JSONObject;

public class Vps4Message {

    public UUID id;
    public UUID accountGuid;
    String typePreFormat;

    public Vps4Message(JSONObject object) {
        id = UUID.fromString(object.get("id").toString());
        typePreFormat = ((JSONObject) object.get("notification")).get("type").toString();
        accountGuid = UUID.fromString(((JSONObject) object.get("notification")).get("account_guid").toString());
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
