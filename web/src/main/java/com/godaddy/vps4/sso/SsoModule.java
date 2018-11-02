package com.godaddy.vps4.sso;

import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequestAuthenticator;
import com.godaddy.vps4.web.security.SsoRequestAuthenticator;
import com.godaddy.vps4.web.security.XCertSubjectHeaderAuthenticator;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class SsoModule extends AbstractModule {

    @Override
    public void configure() {
        Multibinder<RequestAuthenticator<GDUser>> authBinder = Multibinder.newSetBinder(binder(),
                new TypeLiteral<RequestAuthenticator<GDUser>>() {});
        authBinder.addBinding().to(SsoRequestAuthenticator.class);
        authBinder.addBinding().to(XCertSubjectHeaderAuthenticator.class);

        bind(SsoTokenExtractor.class).toProvider(SsoTokenExtractorProvider.class).in(Singleton.class);
    }
}
