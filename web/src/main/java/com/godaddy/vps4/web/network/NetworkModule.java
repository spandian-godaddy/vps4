package com.godaddy.vps4.web.network;

import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.google.inject.AbstractModule;


public class NetworkModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(com.godaddy.vps4.network.NetworkService.class).to(JdbcNetworkService.class);
    }
}
