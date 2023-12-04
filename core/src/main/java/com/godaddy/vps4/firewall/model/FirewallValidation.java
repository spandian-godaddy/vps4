package com.godaddy.vps4.firewall.model;

public class FirewallValidation {
    public String name;
    public String type;
    public String value;

    public FirewallValidation() {}
    public FirewallValidation(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}
