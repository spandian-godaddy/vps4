package com.godaddy.vps4.sso;

import static com.godaddy.vps4.client.ClientUtils.getClientCertAuthServiceProvider;

import javax.inject.Singleton;

import com.godaddy.vps4.sso.clients.Vps4SsoCustomerApi;
import com.google.inject.AbstractModule;

/*
 * The SSO API can be called with client certificates that are whitelisted for specific purposes. The annotations in this
 * class indicate which purpose individual Vps4SsoServices are to be used for.
 */
public class Vps4SsoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Vps4SsoService.class)
                .annotatedWith(Vps4SsoCustomerApi.class)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                                                             "sso.url",
                                                             "customer.api.keyPath",
                                                             "customer.api.certPath"))
                .in(Singleton.class);
    }
}
