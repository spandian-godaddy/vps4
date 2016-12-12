package com.godaddy.vps4.web;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.web.ConfigFactory;
import com.godaddy.vps4.config.Configs;

public class Vps4ConfigFactory implements ConfigFactory {

    @Override
    public Config getInstance() {
        return Configs.getInstance();
    }
}
