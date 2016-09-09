package com.godaddy.vps4.web;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.UserModule;
import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {

        install(new DatabaseModule());

        install(new UserModule());
        bind(UsersResource.class);
        bind(VmsResource.class);
    }
}
