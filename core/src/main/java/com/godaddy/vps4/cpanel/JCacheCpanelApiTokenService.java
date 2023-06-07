package com.godaddy.vps4.cpanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import java.time.Instant;

public class JCacheCpanelApiTokenService implements CpanelApiTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JCacheCpanelApiTokenService.class);

    final CpanelApiTokenService cpanelApiTokenService;

    final Cache<Long, String> cache;

    @Inject
    public JCacheCpanelApiTokenService(
            CpanelApiTokenService cpanelApiTokenService,
            CacheManager cacheManager) {
        this.cpanelApiTokenService = cpanelApiTokenService;
        this.cache = cacheManager.getCache("cpanel.api.token", Long.class, String.class);
    }


    @Override
    public String getApiToken(long vmId, Instant timeoutAt) {
        // first, try to get the api token from the cache
        String apiToken = cache.get(vmId);
        if (apiToken == null) {
            logger.debug("cache not populated for vm {}, deferring to nested cpanel api token service", vmId);

            apiToken = cpanelApiTokenService.getApiToken(vmId, timeoutAt);
            if (apiToken != null) {
                logger.debug("populating cache for vm {}", vmId);
                cache.put(vmId, apiToken);
            }
        }
        return apiToken;
    }

    @Override
    public void invalidateApiToken(long vmId, String apiToken) {
        // if the api token no longer works, remove it from the cache
        logger.debug("invalidate api token for vm {}", vmId);
        cache.remove(vmId);
    }
}
