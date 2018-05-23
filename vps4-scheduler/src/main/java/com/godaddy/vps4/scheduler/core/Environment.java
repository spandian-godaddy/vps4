package com.godaddy.vps4.scheduler.core;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.scheduler.core.config.Configs;

public enum Environment {
    LOCAL,
    JENKINS,
    DEV,
    TEST,
    STAGE,
    PROD,
    PROD_PHX3,
    PROD_SIN2,
    PROD_AMS3;

    private final String localName;

    private Environment() {
        this.localName = name().toLowerCase();
    }

    public String getLocalName() {
        return localName;
    }

    public static final Environment CURRENT = determineCurrent();

    public static final String CONFIG_PROPERTY = "vps4.scheduler.env";

    static Environment determineCurrent() {
        Config config = Configs.getInstance();
        String env = config.get(CONFIG_PROPERTY, "local");
        return Environment.valueOf(env.trim().toUpperCase());
    }
}
