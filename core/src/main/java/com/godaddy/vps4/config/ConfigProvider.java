package com.godaddy.vps4.config;

import javax.inject.Provider;

import com.godaddy.hfs.config.Config;

public class ConfigProvider implements Provider<Config> {

    @Override
    public Config get() {
        return Configs.getInstance();
    }

}
