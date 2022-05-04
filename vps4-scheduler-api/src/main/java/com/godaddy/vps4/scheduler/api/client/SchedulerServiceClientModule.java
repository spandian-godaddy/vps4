package com.godaddy.vps4.scheduler.api.client;

import com.godaddy.vps4.client.Vps4ClientModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.google.inject.AbstractModule;
import static com.godaddy.vps4.client.ClientUtils.getClientCertAuthServiceProvider;


import javax.inject.Singleton;

public class SchedulerServiceClientModule extends AbstractModule {

    @Override
    public void configure() {
        String baseUrlConfigPropName = "vps4.scheduler.url";
        String clientCertKeyPath = "scheduler.api.keyPath";
        String clientCertPath = "scheduler.api.certPath";

        install(new Vps4ClientModule());

        bind(SchedulerWebService.class)
                .toProvider(getClientCertAuthServiceProvider(SchedulerWebService.class,
                        baseUrlConfigPropName,
                        clientCertKeyPath,
                        clientCertPath))
                .in(Singleton.class);
    }


}
