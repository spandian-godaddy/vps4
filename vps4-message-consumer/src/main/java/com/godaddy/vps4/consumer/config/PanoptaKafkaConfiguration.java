package com.godaddy.vps4.consumer.config;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;

public class PanoptaKafkaConfiguration extends KafkaConfiguration {

    @Inject
    public PanoptaKafkaConfiguration(Config vps4Config) {
        super(vps4Config, "vps4.panopta.kafka.topic", "vps4.panopta.kafka.client.id", "vps4.panopta.kafka.bootstrap.servers");
    }

}
