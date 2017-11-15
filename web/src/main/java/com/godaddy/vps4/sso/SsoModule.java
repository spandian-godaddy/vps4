package com.godaddy.vps4.sso;

import java.time.Duration;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.XCertSubjectHeaderAuthenticator;
import com.godaddy.vps4.web.security.RequestAuthenticator;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.HttpKeyService;
import com.godaddy.hfs.sso.KeyService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.security.SsoRequestAuthenticator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class SsoModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SsoModule.class);

    @Override
    public void configure() {
        Multibinder<RequestAuthenticator<GDUser>> authBinder = Multibinder.newSetBinder(binder(),
                new TypeLiteral<RequestAuthenticator<GDUser>>() {});
        authBinder.addBinding().to(SsoRequestAuthenticator.class);
        authBinder.addBinding().to(XCertSubjectHeaderAuthenticator.class);
    }

    @Provides @Singleton
    public SsoTokenExtractor provideTokenExtractor(Config config) {

        // TODO properly configure HTTP client (max routes per host, etc)
        KeyService keyService = new HttpKeyService(
                config.get("sso.url"),
                HttpClientBuilder.create().build());


        long sessionTimeoutMs = Duration.ofSeconds(
                Long.parseLong(
                        config.get(
                                "auth.timeout",
                                String.valueOf(Duration.ofHours(24).getSeconds())))).toMillis();
        logger.info("JWT timeout: {}", sessionTimeoutMs);

        return new SsoTokenExtractor(keyService, sessionTimeoutMs) {

            @Override
            public SsoToken extractToken(HttpServletRequest request) {
                // extract jwt from auth header only, prevents auth_idp cookie lookup for CSRF protection
                return extractAuthorizationHeaderToken(request);
            }
        };
    }
}
