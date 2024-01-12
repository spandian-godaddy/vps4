package com.godaddy.vps4.cpanel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.cache.CacheManager;

import com.godaddy.hfs.cpanel.CPanelService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;


public class CpanelModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Vps4CpanelService.class)
            .to(DefaultVps4CpanelService.class)
            .in(Scopes.SINGLETON);
    }
    @Provides
    public CpanelApiTokenService provideApiTokenService(
            CPanelService cpanelService,
            CacheManager cacheManager) {

        CpanelApiTokenService hfsApiToken = new HfsCpanelApiTokenService(cpanelService, cacheManager);

        CpanelApiTokenService jcacheApiTokenService = new JCacheCpanelApiTokenService(hfsApiToken, cacheManager);

        return jcacheApiTokenService;
    }

}
