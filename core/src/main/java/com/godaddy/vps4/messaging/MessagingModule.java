package com.godaddy.vps4.messaging;

import static com.godaddy.vps4.client.ClientUtils.getCertJwtAuthServiceProvider;

import com.godaddy.vps4.sso.CertJwtApi;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class MessagingModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MessagingService.class).to(DefaultMessagingService.class).in(Singleton.class);
        bind(MessagingApiService.class)
                .toProvider(getCertJwtAuthServiceProvider(MessagingApiService.class,
                                                          "messaging.api.url",
                                                          CertJwtApi.MESSAGING))
                .in(Singleton.class);
    }
}
