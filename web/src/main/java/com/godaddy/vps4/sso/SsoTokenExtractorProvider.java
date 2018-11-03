package com.godaddy.vps4.sso;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.HttpKeyService;
import com.godaddy.hfs.sso.KeyService;
import com.godaddy.hfs.sso.SsoTokenExtractor;

public class SsoTokenExtractorProvider implements Provider<SsoTokenExtractor> {

    private static final Logger logger = LoggerFactory.getLogger(SsoTokenExtractorProvider.class);

    final Config config;

    @Inject
    public SsoTokenExtractorProvider(Config config) {
        this.config = config;
    }

    @Override
    public SsoTokenExtractor get() {
        long ssoTimeoutMs = getSsoTimeoutMs();
        KeyService keyService = getKeyService(config.get("sso.url"));

        String fallbackSsoUrl = config.get("sso.url.ote", null);
        if (fallbackSsoUrl == null) {
            return getSsoTokenExtractor(keyService, ssoTimeoutMs);
        }

        KeyService oteKeyService = getKeyService(fallbackSsoUrl);
        SsoTokenExtractor oteSsoTokenExtractor = getSsoTokenExtractor(oteKeyService, ssoTimeoutMs);
        return getSsoTokenExtractor(keyService, ssoTimeoutMs, oteSsoTokenExtractor);
    }

    SsoTokenExtractor getSsoTokenExtractor(KeyService keyService, long ssoTimeoutMs) {
        return new SsoTokenExtractor(keyService, ssoTimeoutMs);
    }

    SsoTokenExtractor getSsoTokenExtractor(KeyService keyService, long ssoTimeoutMs, SsoTokenExtractor fallbackExtractor) {
        return new FallbackSsoTokenExtractor(keyService, ssoTimeoutMs, fallbackExtractor);
    }

    KeyService getKeyService(String ssoUrl) {
        return new HttpKeyService(ssoUrl, HttpClientBuilder.create().build());
    }

    long getSsoTimeoutMs() {
        long ssoTimeout = Duration.ofHours(24).toMillis(); // Default 1 day
        String configTimeout = config.get("auth.timeout.seconds", null);
        if (configTimeout != null)
            ssoTimeout = Duration.ofSeconds(Long.parseLong(configTimeout)).toMillis();
        logger.info("JWT timeout ms: {}", ssoTimeout);
        return ssoTimeout;
    }

}
