package com.godaddy.vps4.plesk;

public class PleskSubscription {
    
    private String name;

    public PleskSubscription(Object domain) {
        this.name = domain.toString();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "PleskSubscription [domain=" + name + "]";
    }
    
}
