package com.godaddy.vps4.messaging;

import com.godaddy.hfs.config.Config;
import com.google.inject.Inject;

import javax.inject.Provider;

public class MessagingProvider implements Provider<Vps4MessagingService> {

    private final Config config;

    private Vps4MessagingService messagingService;

    @Inject
    public MessagingProvider(Config config) {
        this.config = config;
    }

    @Override
    public Vps4MessagingService get() {
        if (messagingService == null) {
            messagingService = new DefaultVps4MessagingService(config);
        }

        return messagingService;
    }
}
