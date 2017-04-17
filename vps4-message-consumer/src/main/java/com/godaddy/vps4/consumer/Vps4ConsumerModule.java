package com.godaddy.vps4.consumer;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.google.inject.AbstractModule;

public class Vps4ConsumerModule extends AbstractModule {
    @Override
    public void configure() {
        bind(KafkaConfiguration.class);
        bind(Vps4ConsumerManager.class);
    }
}
