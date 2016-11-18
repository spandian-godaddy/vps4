package com.godaddy.vps4.web.cpanel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import gdg.hfs.vhfs.cpanel.CPanelService;

public class CpanelModule extends AbstractModule {

    @Override
    public void configure() {

    }

    @Provides
    public CachedCpanelAccessHashService provideAccessHashService(CPanelService cpanelService) {

        ExecutorService pool = Executors.newCachedThreadPool();

        CpanelAccessHashService hfsAccessHash = new HfsCpanelAccessHashService(cpanelService);

        return new CachedCpanelAccessHashService(pool, hfsAccessHash);
    }

}
