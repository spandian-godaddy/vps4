package com.godaddy.vps4.util;

import com.godaddy.hfs.config.Config;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class MonitoringProvider implements Provider<Monitoring> {

    private final Monitoring monitoring;

    @Inject
    public MonitoringProvider(Config config) {
        monitoring = new Monitoring(config);
    }

    @Override
    public Monitoring get() {
        return monitoring;
    }
}
