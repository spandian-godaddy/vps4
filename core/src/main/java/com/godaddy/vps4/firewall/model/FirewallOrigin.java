package com.godaddy.vps4.firewall.model;

public class FirewallOrigin {
    public String domain;
    public String address;
    public int port;
    public boolean tlsEnabled;

    public FirewallOrigin() {
    }

    public FirewallOrigin(String domain, String address, int port, boolean tlsEnabled) {
        this.domain = domain;
        this.address = address;
        this.port = port;
        this.tlsEnabled = tlsEnabled;
    }
}
