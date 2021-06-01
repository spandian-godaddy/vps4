package com.godaddy.vps4.ipblacklist;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.json.simple.JSONObject;

import com.godaddy.hfs.config.Config;

public class DefaultIpBlacklistService implements IpBlacklistService {

    private final Config config;
    private final IpBlacklistClientService blacklist;

    @Inject
    public DefaultIpBlacklistService(Config config, IpBlacklistClientService blacklist)
    {
        this.config = config;
        this.blacklist = blacklist;
    }

    @Override
    public void removeIpFromBlacklist(String ip) {
        try{
            blacklist.deleteBlacklistRecord(ip);
        }
        catch (NotFoundException e)
        {
            //Do nothing on not found exception - the IP isn't blacklisted so this method should succeed
        }
    }

    @Override
    public boolean isIpBlacklisted(String ip) {
        boolean isBlacklisted = true;
        try {
            JSONObject result = blacklist.getBlacklistRecord(ip);
            isBlacklisted = result.containsKey("data") ? true : false;
        }
        catch (NotFoundException e) {
            isBlacklisted = false;
        }
        return isBlacklisted;
    }

    @Override
    public void blacklistIp(String ip) {
        blacklist.createBlacklistRecord(ip);
    }
}
