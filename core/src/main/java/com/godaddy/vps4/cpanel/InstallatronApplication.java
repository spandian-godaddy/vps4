package com.godaddy.vps4.cpanel;

public class InstallatronApplication {
    public String name;
    public String id;
    public String domain;
    public String version;

    public InstallatronApplication(String name, String id, String domain, String version) {
        this.name = name;
        this.id = id;
        this.domain = domain;
        this.version = version;
    }
}
