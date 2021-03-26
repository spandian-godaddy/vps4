package com.godaddy.hfs.cpanel;

public class CPanelLicense {
    public long vmId;
    public String licensedIp;

    @Override
    public String toString() {
        return "CPanelLicense [vmId=" + vmId +
                ", licensedIp=" + licensedIp +
                "]";
    }
}
