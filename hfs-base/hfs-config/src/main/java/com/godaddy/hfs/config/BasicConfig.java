package com.godaddy.hfs.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicConfig implements Config {

    protected final Map<String, String> properties = new ConcurrentHashMap<>();

    protected final Map<String, byte[]> data = new ConcurrentHashMap<>();

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
            throw new IllegalStateException("Missing config: " + key);
        }
        return value;
    }

    @Override
    public byte[] getData(String path) {
        byte[] data = this.data.get(path);
        if (data == null && parent != null) {
            data = parent.getData(path);
        }
        return data;
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    public void setData(String key, byte[] value) {
        data.put(key, value);
    }
    
}
