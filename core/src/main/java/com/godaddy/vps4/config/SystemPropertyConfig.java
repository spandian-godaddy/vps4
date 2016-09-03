package com.godaddy.vps4.config;

/**
 * System Properties override the given parent Config
 *
 */
public class SystemPropertyConfig extends BasicConfig {

    public SystemPropertyConfig(Config parent) {
        super(parent);
    }

    public SystemPropertyConfig() {
        super(null);
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = System.getProperty(key, null);
        if (value == null) {
            value = super.get(key, defaultValue);
        }
        return value;
    }

}
