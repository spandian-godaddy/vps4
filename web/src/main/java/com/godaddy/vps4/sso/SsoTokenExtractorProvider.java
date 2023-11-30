package com.godaddy.vps4.sso;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
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
            return getSsoTokenExtractor(ssoService);
        }

        SsoService oteSsoService = getKeyService(fallbackSsoUrl);
        SsoTokenExtractor oteSsoTokenExtractor = getSsoTokenExtractor(oteSsoService);
        return getSsoTokenExtractor(ssoService, oteSsoTokenExtractor);
    }

    SsoTokenExtractor getSsoTokenExtractor(SsoService ssoService) {
        return new Vps4SsoTokenExtractor(ssoService);
    }

    SsoTokenExtractor getSsoTokenExtractor(SsoService ssoService, SsoTokenExtractor fallbackExtractor) {
        return new FallbackSsoTokenExtractor(ssoService, fallbackExtractor);
    }

    SsoTokenExtractor getLegacySsoTokenExtractor(SsoService ssoService, long ssoTimeoutMs) {
        return new Vps4LegacySsoTokenExtractor(ssoService, ssoTimeoutMs);
    }

    SsoService getKeyService(String ssoUrl) {
        int timeout = Integer.parseInt(config.get("http.sso.timeout.seconds", "30"));
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
        return new HttpSsoService(ssoUrl, httpClient);
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
