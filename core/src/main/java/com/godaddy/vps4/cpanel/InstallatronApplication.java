package com.godaddy.vps4.cpanel;

public class InstallatronApplication {
    public String name;
    public String id;
    public String domain;
    public String urlDomain;
    public String version;

    public InstallatronApplication(String name, String id, String domain, String urlDomain, String version) {
        this.name = name;
        this.id = id;
        this.domain = domain;
        this.urlDomain = urlDomain;
        this.version = version;
    }
}
