package com.godaddy.vps4.cpanel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import gdg.hfs.vhfs.cpanel.CPanelService;

public class CpanelModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Vps4CpanelService.class)
            .to(DefaultVps4CpanelService.class)
            .in(Scopes.SINGLETON);
    }

    @Provides
    public CpanelAccessHashService provideAccessHashService(CPanelService cpanelService) {

        ExecutorService pool = Executors.newCachedThreadPool();

        CpanelAccessHashService hfsAccessHash = new HfsCpanelAccessHashService(cpanelService);

        return new CachedCpanelAccessHashService(pool, hfsAccessHash);
    }

}
