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
            logger.info("cache not populated for vm {}, deferring to nested cpanel api token service", vmId);

            apiToken = cpanelApiTokenService.getApiToken(vmId, timeoutAt);
            if (apiToken != null) {
                logger.info("populating cache for vm {}", vmId);
                cache.put(vmId, apiToken);
            }
        }
        return apiToken;
    }

    @Override
    public void invalidateApiToken(long vmId, String apiToken) {
        // call nested invalidation method
        cpanelApiTokenService.invalidateApiToken(vmId, apiToken);
        // if the api token no longer works, remove it from the cache
        logger.info("invalidate api token for vm {}", vmId);
        boolean removed = cache.remove(vmId,apiToken);
        if (removed) {
            logger.info("successfully invalidated api token for vm {}", vmId);
        }
        logger.info("cache is holding onto a different api token for vm {}", vmId);
    }
}
