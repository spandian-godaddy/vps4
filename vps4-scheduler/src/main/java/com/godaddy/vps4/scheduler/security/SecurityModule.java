package com.godaddy.vps4.scheduler.security;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class SecurityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<RequestAuthenticator<Boolean>>() {}).to(XCertSubjectHeaderAuthenticator.class);
    }
}