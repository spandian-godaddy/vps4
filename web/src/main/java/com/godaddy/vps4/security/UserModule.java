package com.godaddy.vps4.security;

import com.google.inject.AbstractModule;

public class UserModule extends AbstractModule {

    @Override
    protected void configure() {
        User user = new User();
        user.name = "Brian";

        bind(User.class).toInstance(user);
    }

}
