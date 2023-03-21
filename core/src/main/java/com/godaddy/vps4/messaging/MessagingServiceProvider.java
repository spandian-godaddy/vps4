package com.godaddy.vps4.messaging;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Provider;

public class MessagingServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    public MessagingServiceProvider(Class<T> serviceClass) {
        super("messaging.api.url", serviceClass);
    }
}
