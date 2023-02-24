package com.godaddy.vps4.messaging;

import com.google.inject.AbstractModule;

public class MessagingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Vps4MessagingService.class).to(DefaultVps4MessagingService.class);
    }
}
