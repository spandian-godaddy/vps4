package com.godaddy.vps4.sso;

import java.time.Duration;

import javax.inject.Singleton;

import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.HttpKeyService;
import com.godaddy.hfs.sso.KeyService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.vps4.web.security.RequestAuthenticator;
import com.godaddy.vps4.web.security.SsoRequestAuthenticator;
import com.godaddy.vps4.web.security.Vps4RequestAuthenticator;
import com.godaddy.vps4.web.security.Vps4SupportRequestAuthenticator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class SsoModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SsoModule.class);

    @Override
    public void configure() {
        bind(RequestAuthenticator.class).to(Vps4SupportRequestAuthenticator.class);
        bind(Vps4RequestAuthenticator.class);
        bind(SsoRequestAuthenticator.class);
    }

    @Provides @Singleton
    public SsoSupportTokenExtractor provideSupportTokenExtractor(Config config) {

        KeyService keyService = new HttpKeyService(
                config.get("sso.url"),
                HttpClientBuilder.create().build());

        return new SsoSupportTokenExtractor(keyService);
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

        return new SsoTokenExtractor(keyService, sessionTimeoutMs);
    }
}
