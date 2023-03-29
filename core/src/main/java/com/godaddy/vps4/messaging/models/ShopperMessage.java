package com.godaddy.vps4.messaging.models;

import java.util.EnumMap;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopperMessage {
    public String templateNamespaceKey = "Hosting";
    public TemplateType templateTypeKey;
    public EnumMap<Substitution, String> substitutionValues;
    public EnumMap<Transformation, String> transformationData;

    // Empty constructor required for Jackson
    public ShopperMessage() {}

    public ShopperMessage(TemplateType templateTypeKey) {
        this.templateTypeKey = templateTypeKey;
    }

    public ShopperMessage substitute(Substitution key, String value) {
        if (substitutionValues == null) {
            substitutionValues = new EnumMap<>(Substitution.class);
        }
        substitutionValues.put(key, value);
        return this;
    }

    public void transform(Transformation key, String value) {
        if (transformationData == null) {
            transformationData = new EnumMap<>(Transformation.class);
        }
        transformationData.put(key, value);
    }
}
