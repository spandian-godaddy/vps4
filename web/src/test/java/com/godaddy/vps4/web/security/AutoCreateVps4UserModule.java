package com.godaddy.vps4.web.security;

import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class AutoCreateVps4UserModule extends AbstractModule {

    final String shopperId;

    public AutoCreateVps4UserModule(String shopperId) {
        this.shopperId = shopperId;
    }

    @Override
    protected void configure() {

    }

    @Provides
    protected Vps4User provideUser(Vps4UserService userService) {

        return userService.getOrCreateUserForShopper(shopperId, "1");
    }

}
