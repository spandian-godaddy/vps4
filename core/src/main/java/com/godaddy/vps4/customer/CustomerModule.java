package com.godaddy.vps4.customer;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class CustomerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CustomerService.class).to(DefaultCustomerService.class).in(Scopes.SINGLETON);
        bind(CustomerApiService.class).toProvider(new CustomerServiceProvider<>(CustomerApiService.class));
    }
}
