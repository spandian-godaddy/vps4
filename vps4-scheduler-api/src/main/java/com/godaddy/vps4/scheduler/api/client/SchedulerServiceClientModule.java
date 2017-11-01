package com.godaddy.vps4.scheduler.api.client;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.client.ClientCertAuthenticatedServiceProvider;
import com.godaddy.vps4.client.Vps4ClientModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class SchedulerServiceClientModule extends AbstractModule {

    @Override
    public void configure() {
        install(new Vps4ClientModule());
        bind(SchedulerWebService.class)
            .annotatedWith(ClientCertAuth.class)
            .toProvider(getSchedulerServiceProvider())
            .in(Singleton.class);
    }

    private ClientCertAuthenticatedServiceProvider<SchedulerWebService> getSchedulerServiceProvider() {
        return new ClientCertAuthenticatedServiceProvider<>(
            "vps4.scheduler.url",
            SchedulerWebService.class,
            "hfs.client.keyPath", // TODO: change this to the scheduler client key path
            "hfs.client.certPath"); // TODO: change this to the scheduler client cert path
    }
}
