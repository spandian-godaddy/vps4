package com.godaddy.vps4.cpanel;

public class CPanelAccountCacheStatus {
    public String username;
    public boolean isEnabled;

    public CPanelAccountCacheStatus(String username, boolean isEnabled) {
        this.username = username;
        this.isEnabled = isEnabled;
    }
}
