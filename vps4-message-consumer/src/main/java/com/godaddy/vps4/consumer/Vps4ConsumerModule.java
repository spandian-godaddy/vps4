package com.godaddy.vps4.consumer;

import com.godaddy.vps4.consumer.config.AccountKafkaConfiguration;
import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.consumer.config.MonitoringKafkaConfiguration;
import com.godaddy.vps4.consumer.config.ZookeeperConfig;
import com.godaddy.vps4.handler.BasicMessageHandler;
import com.godaddy.vps4.handler.MessageHandler;
import com.godaddy.vps4.handler.Vps4AccountMessageHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class Vps4ConsumerModule extends AbstractModule {
    @Override
    public void configure() {
        bind(ZookeeperConfig.class);
        bind(MessageHandler.class).annotatedWith(Names.named("Account")).to(Vps4AccountMessageHandler.class).in(Scopes.SINGLETON);
        bind(MessageHandler.class).annotatedWith(Names.named("Monitoring")).to(BasicMessageHandler.class).in(Scopes.SINGLETON);
        bind(KafkaConfiguration.class).annotatedWith(Names.named("Account")).to(AccountKafkaConfiguration.class).in(Scopes.SINGLETON);
        bind(KafkaConfiguration.class).annotatedWith(Names.named("Monitoring")).to(MonitoringKafkaConfiguration.class).in(Scopes.SINGLETON);
    }
}
