package com.godaddy.vps4.consumer.config;

import com.godaddy.hfs.config.Config;
import javax.inject.Inject;

public class MonitoringKafkaConfiguration extends KafkaConfiguration {
    
    @Inject
    public MonitoringKafkaConfiguration(Config vps4Config) {
        super(vps4Config, "vps4.monitoring.kafka.topic", "vps4.monitoring.kafka.client.id");
    }
}