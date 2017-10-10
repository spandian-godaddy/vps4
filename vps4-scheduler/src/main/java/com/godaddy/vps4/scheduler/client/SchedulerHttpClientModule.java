package com.godaddy.vps4.scheduler.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.util.ObjectMapperProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class SchedulerHttpClientModule extends AbstractModule {

    @Override
    protected void configure() {
        // TODO: add any required bindings
    }

    @Provides
    @Inject
    protected SchedulerClientService provideSchedulerClient(Config config) {

        String baseUrl = config.get("vps4.scheduler.url");
        CloseableHttpClient client = HttpClients.createDefault();
        ObjectMapper mapper = new ObjectMapperProvider().get();

        return new SchedulerClientHttpService(baseUrl, client, mapper);

    }

}
