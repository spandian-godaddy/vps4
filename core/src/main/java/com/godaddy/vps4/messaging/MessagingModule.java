package com.godaddy.vps4.messaging;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class MessagingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessagingService.class).to(DefaultMessagingService.class).in(Singleton.class);
        bind(MessagingApiService.class).toProvider(new MessagingServiceProvider<>(MessagingApiService.class))
                                       .in(Singleton.class);
    }
}
