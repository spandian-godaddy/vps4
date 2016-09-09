package com.godaddy.vps4.security;

import com.google.inject.AbstractModule;

public class UserModule extends AbstractModule {

    @Override
    protected void configure() {
        User user = new User("Brian", 0, "shopperId");

        bind(User.class).toInstance(user);
    }

}
