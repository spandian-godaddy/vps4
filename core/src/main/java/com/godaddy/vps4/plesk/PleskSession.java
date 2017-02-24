package com.godaddy.vps4.plesk;

public class PleskSession {
    
    private String ssoUrl;

    public PleskSession(String ssoUrl) {
        this.setSsoUrl(ssoUrl);
    }

    public String getSsoUrl() {
        return ssoUrl;
    }

    public void setSsoUrl(String ssoUrl) {
        this.ssoUrl = ssoUrl;
    }
    
    @Override
    public String toString() {
        return "PleskSession [ssoUrl=" + ssoUrl + "]";
    }
}
