package com.godaddy.vps4.security;

import com.google.inject.AbstractModule;

// TODO refactor into SecurityModule, manage things like privileges
public class UserModule extends AbstractModule {

    @Override
    protected void configure() {
        User user = new User("SomeUser", 42, "someShopperId");

        bind(User.class).toInstance(user);
    }

}