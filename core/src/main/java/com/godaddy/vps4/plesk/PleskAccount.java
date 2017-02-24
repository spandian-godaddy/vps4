package com.godaddy.vps4.plesk;

public class PleskAccount {
    
    private String domain;
    private String webspace;
    private String ipAddress;
    private String ftpLogin;
    private String diskUsed;

    public PleskAccount(String domain, String webspace, String ipAddress, String ftpLogin, String diskUsed) {
        this.domain = domain;
        this.webspace = webspace;
        this.ipAddress = ipAddress;
        this.ftpLogin = ftpLogin;
        this.diskUsed = diskUsed;
    }

    public String getName() {
        return domain;
    }

    public void setName(String domain) {
        this.domain = domain;
    }

    public String getWebspace() {
        return webspace;
    }

    public void setWebspace(String webspace) {
        this.webspace = webspace;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getFtpLogin() {
        return ftpLogin;
    }

    public void setFtpLogin(String ftpLogin) {
        this.ftpLogin = ftpLogin;
    }

    public String getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(String diskUsed) {
        this.diskUsed = diskUsed;
    }

    @Override
    public String toString() {
        return "PleskAccount [domain=" + domain + ", webspace=" + webspace + ", ipAddress=" + ipAddress + ", ftpLogin=" + ftpLogin + ", diskUsed=" + diskUsed + "]";
    }
    
}
