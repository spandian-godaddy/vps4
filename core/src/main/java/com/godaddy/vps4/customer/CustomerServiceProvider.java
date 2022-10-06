package com.godaddy.vps4.customer;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Provider;

public class CustomerServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    public CustomerServiceProvider(Class<T> serviceClass) {
        super("customer.api.url", serviceClass);
    }
}
