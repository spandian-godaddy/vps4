package com.godaddy.vps4.sso;

import static com.godaddy.vps4.client.ClientUtils.getClientCertAuthServiceProvider;

import com.godaddy.vps4.sso.clients.Vps4SsoMessagingApi;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/*
 * The SSO API can be called with client certificates that are whitelisted for specific purposes. The annotations in
 * this class indicate which purpose individual Vps4SsoServices are to be used for.
 */
public class Vps4SsoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Vps4SsoService.class)
                .annotatedWith(Vps4SsoMessagingApi.class)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                                                             "sso.url",
                                                             "messaging.api.keyPath",
                                                             "messaging.api.certPath"))
                .in(Singleton.class);
    }
}
