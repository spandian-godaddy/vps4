package com.godaddy.vps4.web;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.jackson.ObjectMapperProvider;
import gdg.hfs.orchestration.client.HttpCommandService;

public class CommandClientModule extends AbstractModule {

    @Override
    public void configure() {

    }

    @Provides
    CommandService provideCommandService(Config config) {

        String baseUrl = config.get("orchestration.url");
        CloseableHttpClient client = HttpClients.createDefault();
        ObjectMapper mapper = new ObjectMapperProvider().get();

        return new HttpCommandService(baseUrl, client, mapper);

    }
}
