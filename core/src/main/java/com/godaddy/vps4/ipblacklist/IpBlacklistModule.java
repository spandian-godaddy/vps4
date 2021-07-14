package com.godaddy.vps4.ipblacklist;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class IpBlacklistModule extends AbstractModule {
    @Override
    public void configure() {
        install(new IpBlacklistServiceModule());
        bind(IpBlacklistService.class).to(DefaultIpBlacklistService.class).in(Scopes.SINGLETON);
    }
}
