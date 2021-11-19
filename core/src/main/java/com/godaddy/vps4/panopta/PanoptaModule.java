package com.godaddy.vps4.panopta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PanoptaModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new PanoptaClientModule());
        install(new PanoptaDataModule());
        bind(ExecutorService.class).annotatedWith(PanoptaExecutorService.class).toInstance(Executors.newCachedThreadPool());
        bind(PanoptaService.class).to(DefaultPanoptaService.class).in(Scopes.SINGLETON);
        bind(PanoptaMetricMapper.class);
    }
}
