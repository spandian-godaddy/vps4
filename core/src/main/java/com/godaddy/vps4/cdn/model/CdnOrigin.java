package com.godaddy.vps4.cdn.model;

public class CdnOrigin {
    public String domain;
    public String address;
    public int port;
    public boolean tlsEnabled;

    public CdnOrigin() {
    }

    public CdnOrigin(String domain, String address, int port, boolean tlsEnabled) {
        this.domain = domain;
        this.address = address;
        this.port = port;
        this.tlsEnabled = tlsEnabled;
    }
}
