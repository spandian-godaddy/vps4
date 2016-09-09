package com.godaddy.vps4.web;

import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {

        bind(VmsResource.class);
    }
}
