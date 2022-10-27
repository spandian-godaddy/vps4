package com.godaddy.vps4.messaging;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.customer.CustomerService;
import com.google.inject.Inject;

import javax.inject.Provider;

public class MessagingProvider implements Provider<Vps4MessagingService> {

    private final Config config;

    private Vps4MessagingService messagingService;
    private CustomerService customerService;

    @Inject
    public MessagingProvider(Config config, CustomerService customerService) {
        this.config = config;
        this.customerService = customerService;
    }

    @Override
    public Vps4MessagingService get() {
        if (messagingService == null) {
            messagingService = new DefaultVps4MessagingService(config, customerService);
        }

        return messagingService;
    }
}
