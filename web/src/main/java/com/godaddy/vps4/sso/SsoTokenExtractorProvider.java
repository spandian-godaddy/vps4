package com.godaddy.vps4.sso;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.HttpSsoService;
import com.godaddy.hfs.sso.SsoService;
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
        SsoService ssoService = getKeyService(config.get("sso.url"));

        boolean allowJomaxCookie = Boolean.parseBoolean(config.get("sso.cookies.allowJomax", null));
        String fallbackSsoUrl = config.get("sso.url.ote", null);
        if (allowJomaxCookie) {
            return getLegacySsoTokenExtractor(ssoService, ssoTimeoutMs);
        } else if (fallbackSsoUrl == null) {
            return getSsoTokenExtractor(ssoService, ssoTimeoutMs);
        }

        SsoService oteSsoService = getKeyService(fallbackSsoUrl);
        SsoTokenExtractor oteSsoTokenExtractor = getSsoTokenExtractor(oteSsoService, ssoTimeoutMs);
        return getSsoTokenExtractor(ssoService, ssoTimeoutMs, oteSsoTokenExtractor);
    }

    SsoTokenExtractor getSsoTokenExtractor(SsoService ssoService, long ssoTimeoutMs) {
        return new Vps4SsoTokenExtractor(ssoService, ssoTimeoutMs);
    }

    SsoTokenExtractor getSsoTokenExtractor(SsoService ssoService, long ssoTimeoutMs, SsoTokenExtractor fallbackExtractor) {
        return new FallbackSsoTokenExtractor(ssoService, ssoTimeoutMs, fallbackExtractor);
    }

    SsoTokenExtractor getLegacySsoTokenExtractor(SsoService ssoService, long ssoTimeoutMs) {
        return new Vps4LegacySsoTokenExtractor(ssoService, ssoTimeoutMs);
    }

    SsoService getKeyService(String ssoUrl) {
        return new HttpSsoService(ssoUrl, HttpClientBuilder.create().build());
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
