package com.godaddy.vps4.messaging;

import com.google.inject.AbstractModule;

public class MessagingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessagingService.class).to(DefaultMessagingService.class);
        bind(MessagingApiService.class).toProvider(new MessagingServiceProvider<>(MessagingApiService.class));
    }
}
