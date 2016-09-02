package com.godaddy.vps4.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicConfig implements Config {

    private final Map<String, String> properties = new ConcurrentHashMap<>();

    private final Config parent;

    public BasicConfig() {
        this(null);
    }

    public BasicConfig(Config parent) {
        this.parent = parent;
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = properties.get(key);

        if (value == null && parent != null) {
            value = parent.get(key, null);
        }

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    @Override
    public String get(String key) {
        String value = get(key, null);
        if (value == null) {
            throw new IllegalStateException("Missing config: " + value);
        }
        return value;
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

}
