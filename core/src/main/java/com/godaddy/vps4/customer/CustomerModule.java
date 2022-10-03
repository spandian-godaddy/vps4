package com.godaddy.vps4.customer;

import com.google.inject.AbstractModule;

public class CustomerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CustomerService.class).toProvider(new CustomerServiceProvider<>(CustomerService.class));
        bind(CustomerServiceRequestFilter.class);
    }
}
