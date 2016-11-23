package com.godaddy.vps4.config;

public interface Config {

    String get(String key, String defaultValue);

    String get(String key);

    byte[] getData(String path);
}
