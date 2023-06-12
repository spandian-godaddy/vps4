package com.godaddy.vps4.util;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class UtilsModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Cryptography.class).toProvider(CryptographyProvider.class).in(Scopes.SINGLETON);
        bind(NetworkService.class).to(JdbcNetworkService.class);
        bind(TroubleshootVmService.class).to(DefaultTroubleshootVmService.class).in(Scopes.SINGLETON);
    }

}
