package com.godaddy.vps4.ipblacklist;

public interface IpBlacklistService {
    public void removeIpFromBlacklist(String ip);

    public boolean isIpBlacklisted(String ip);

    public void blacklistIp(String ip);
}
