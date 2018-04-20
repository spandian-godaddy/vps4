package com.godaddy.vps4.consumer.config;

import com.godaddy.hfs.config.Config;
import javax.inject.Inject;

public class AccountKafkaConfiguration extends KafkaConfiguration {
    
    @Inject
    public AccountKafkaConfiguration(Config vps4Config) {
        super(vps4Config, "vps4.account.kafka.topic", "vps4.account.kafka.client.id");
    }
}