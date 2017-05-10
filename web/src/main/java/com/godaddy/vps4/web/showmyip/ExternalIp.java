package com.godaddy.vps4.web.showmyip;

public class ExternalIp {
    private String ipAddress;

    public ExternalIp(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
