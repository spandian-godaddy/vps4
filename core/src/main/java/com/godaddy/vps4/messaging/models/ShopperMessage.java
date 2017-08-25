package com.godaddy.vps4.messaging.models;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopperMessage {

    public String templateNamespaceKey;
    public String templateTypeKey;
    public ShopperOverride shopperOverride;
    public List<EmailRecipient> additionalRecipients = null;
    public EnumMap<?, String> substitutionValues;
    public EnumMap<?, String> transformationData;
    public Boolean sendToShopper;
    public ShopperNote shopperNote;

    @Override
    public String toString() {
        return "ShopperMessage [templateNamespaceKey: " + templateNamespaceKey +
                " templateTypeKey: " + templateTypeKey +
                " sendToShopper: " + sendToShopper + " shopperNote: " + shopperNote.toString() +
                " shopperOverride: " + shopperOverride.toString() + "]";
    }
}
