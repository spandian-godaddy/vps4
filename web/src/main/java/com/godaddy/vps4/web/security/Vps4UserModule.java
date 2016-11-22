package com.godaddy.vps4.web.security;

import javax.servlet.http.HttpServletRequest;

import com.godaddy.vps4.security.Vps4User;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class Vps4UserModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    protected Vps4User provideUser(HttpServletRequest request) {
        return (Vps4User) request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME);
    }

}
