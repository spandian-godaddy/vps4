package com.godaddy.hfs.web;

import com.google.inject.AbstractModule;

public class SimpleWebServerModule extends AbstractModule {

    @Override
    public void configure() {
        install(new ServerModule());
        install(new HttpModule());
        install(new HttpsModule());
    }
}
