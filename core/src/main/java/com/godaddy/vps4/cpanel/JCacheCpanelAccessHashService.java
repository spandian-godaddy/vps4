package com.godaddy.vps4.cpanel;

import java.time.Instant;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCacheCpanelAccessHashService implements CpanelAccessHashService {

    private static final Logger logger = LoggerFactory.getLogger(JCacheCpanelAccessHashService.class);

    final CpanelAccessHashService accessHashService;

    final Cache<Long, String> cache;

    @Inject
    public JCacheCpanelAccessHashService(
            CpanelAccessHashService accessHashService,
            CacheManager cacheManager) {
        this.accessHashService = accessHashService;
        this.cache = cacheManager.getCache("cpanel.accesshash", Long.class, String.class);
    }

    @Override
    public String getAccessHash(long vmId, String publicIp, String fromIp, Instant timeoutAt) {

        // first, try to get the access hash from the cache
        String accessHash = cache.get(vmId);
        if (accessHash == null) {
            logger.debug("cache not populated for vm {}, deferring to nested accesshash service", vmId);

            accessHash = accessHashService.getAccessHash(vmId, publicIp, fromIp, timeoutAt);
            if (accessHash != null) {
                logger.debug("populating cache for vm {}", vmId);
                cache.put(vmId, accessHash);
            }
        }

        return accessHash;
    }

    @Override
    public void invalidAccessHash(long vmId, String accessHash) {

        // if the access hash no longer works, remove it from the cache
        logger.debug("invalidate access hash for vm {}", vmId);
        cache.remove(vmId);
    }

}
