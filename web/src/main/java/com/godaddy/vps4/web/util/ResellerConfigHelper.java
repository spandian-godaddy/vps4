package com.godaddy.vps4.web.util;

import com.godaddy.hfs.config.Config;

public class ResellerConfigHelper{

    // Return the config.resellerId value.
    // If that doesn't exist, return the config value
    // If that doesn't exist, return the defaultValue
    public static String getResellerConfig(Config config, String resellerId, String configName, String defaultValue){
        String defaultConfigValue = config.get(configName, defaultValue);
        return config.get(configName + "." + resellerId, defaultConfigValue);
    }

}