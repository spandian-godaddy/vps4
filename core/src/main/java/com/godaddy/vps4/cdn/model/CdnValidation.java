package com.godaddy.vps4.cdn.model;

public class CdnValidation {
    public String name;
    public String type;
    public String value;

    public CdnValidation() {}
    public CdnValidation(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}
