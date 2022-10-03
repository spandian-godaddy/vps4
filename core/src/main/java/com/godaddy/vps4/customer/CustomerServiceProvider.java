package com.godaddy.vps4.customer;

import java.util.List;

import javax.ws.rs.client.ClientRequestFilter;

import com.godaddy.vps4.client.HttpServiceProvider;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CustomerServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    @Inject CustomerServiceRequestFilter customerServiceRequestFilter;

    public CustomerServiceProvider(Class<T> serviceClass) {
        super("customer.api.url", serviceClass);
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(customerServiceRequestFilter);
        return requestFilters;
    }

    @Override
    public T get() {
        return super.get();
    }
}
