package com.godaddy.vps4;

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

    public static final Environment CURRENT = getCurrent();

    public static Environment getCurrent() {
        String env = System.getProperty("vps4.env", "local");

        return Environment.valueOf(env.trim().toUpperCase());
    }
}
