package com.godaddy.vps4.scheduler.core.config;

import com.godaddy.hfs.config.Config;

import javax.inject.Provider;

public class ConfigProvider implements Provider<Config> {

    @Override
    public Config get() {
        return Configs.getInstance();
    }

}
