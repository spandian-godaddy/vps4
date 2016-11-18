package com.godaddy.vps4.config;

import java.time.Duration;

public interface Config {

    String get(String key, String defaultValue);

    String get(String key);

    Duration getDuration(String configProperty, Duration defaultPropertyValue);
}
