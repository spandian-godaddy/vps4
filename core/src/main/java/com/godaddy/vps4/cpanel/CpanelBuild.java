package com.godaddy.vps4.cpanel;


public class CpanelBuild {
    public long buildNumber;
    public String packageName;

    public CpanelBuild(Long buildNumber, String packageName) {
        this.buildNumber = buildNumber;
        this.packageName = packageName;
    }
}
