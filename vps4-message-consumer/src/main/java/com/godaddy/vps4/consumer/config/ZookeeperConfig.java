package com.godaddy.vps4.consumer.config;

import com.godaddy.hfs.config.Config;

import javax.inject.Inject;

public class ZookeeperConfig {

    public final String serviceName;

    public final String path;

    @Inject
    public ZookeeperConfig(Config vps4Config) {

        this.serviceName = vps4Config.get("servicediscovery.zk.serviceName", "vps4-message-consumer");
        this.path = vps4Config.get("servicediscovery.zk.path", "/service/vps4/vps4-message-consumer/");
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPath() {
        return path;
    }

}
