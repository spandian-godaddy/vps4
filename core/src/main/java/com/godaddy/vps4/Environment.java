package com.godaddy.vps4;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.config.Configs;

public enum Environment {
    LOCAL,
    JENKINS,
    DEV,
    TEST,
    STAGE,
    PROD
    ;

    private final String localName;

    private Environment() {
        this.localName = name().toLowerCase();
    }

    public String getLocalName() {
        return localName;
    }

    public static final Environment CURRENT = determineCurrent();

    public static final String CONFIG_PROPERTY = "vps4.env";

    static Environment determineCurrent() {

        Config config = Configs.getInstance();

        String env = config.get(CONFIG_PROPERTY, "local");

        return Environment.valueOf(env.trim().toUpperCase());
    }
}
