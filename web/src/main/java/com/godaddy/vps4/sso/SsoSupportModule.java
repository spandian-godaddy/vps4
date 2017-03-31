package com.godaddy.vps4.sso;

import com.godaddy.vps4.web.security.Vps4SupportRequestAuthenticator;
import com.google.inject.AbstractModule;

public class SsoSupportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Vps4SupportRequestAuthenticator.class);
    }

}
