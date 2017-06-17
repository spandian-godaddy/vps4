package com.godaddy.vps4.web.security;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class GDUserModule extends AbstractModule {

    @Override
    public void configure() {
    }

    @Provides
    protected GDUser provideUser(HttpServletRequest request) {
        return (GDUser) request.getAttribute(SsoAuthenticationFilter.USER_ATTRIBUTE_NAME);
    }

}
