package com.godaddy.vps4.web.log;

import com.google.inject.AbstractModule;

public class LogModule extends AbstractModule {

    @Override
    public void configure() {
        bind(LogLevelListener.class);
    }
}