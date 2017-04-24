package com.godaddy.vps4.consumer;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.handler.MessageHandler;
import com.godaddy.vps4.handler.Vps4MessageHandler;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class Vps4ConsumerModule extends AbstractModule {
    @Override
    public void configure() {
        bind(KafkaConfiguration.class);
        //bind(Vps4ConsumerGroup.class);
        bind(MessageHandler.class).to(Vps4MessageHandler.class).in(Scopes.SINGLETON);
    }
}
