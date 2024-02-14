package com.godaddy.vps4.cpanel;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelService;
import com.godaddy.vps4.cache.CacheName;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.time.Instant;

public class HfsCpanelApiTokenService implements CpanelApiTokenService {

    private static final Logger logger = LoggerFactory.getLogger(HfsCpanelApiTokenService.class);

    final CPanelService cPanelService;

    final Cache<Long, Long> cache;

    public HfsCpanelApiTokenService(CPanelService cPanelService,
                                    CacheManager cacheManager) {
        this.cPanelService = cPanelService;
        this.cache = cacheManager.getCache(CacheName.CPANEL_API_TOKEN_ACTION, Long.class, Long.class);

    }

    @Override
    public void invalidateApiToken(long vmId, String apiToken) {
        cache.remove(vmId); // remove cached hfs actionId in JCache
    }

    @Override
    public String getApiToken(long vmId, Instant timeoutAt) {
        try {
            return getApiTokenFromHFS(vmId, timeoutAt);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void removeCachedActionId(Long vmId, Long actionId) {
        boolean removed = cache.remove(vmId, actionId);
        if (removed) {
            logger.info("successfully invalidated cached token action for vm {}", vmId);
        }
        else {
            logger.info("cache is holding onto a different token action for vm {}", vmId); 
        }
    }

    String pollForHfsAction(CPanelAction hfsAction, Instant timeoutAt, long vmId) throws InterruptedException {
        while (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)
                && !hfsAction.status.equals(CPanelAction.Status.FAILED)
                && Instant.now().isBefore(timeoutAt)) {
            logger.info("waiting on generate cpanel API token: {}", hfsAction);
            Thread.sleep(1000);
            hfsAction = cPanelService.getAction(hfsAction.actionId);
        }

        if (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)) {
            logger.warn("failed to generate API token {}, removing cached token action for vm Id {}", hfsAction, vmId);
            removeCachedActionId(vmId, hfsAction.actionId);
            throw new RuntimeException("CPanel generate API token failed");
        }

        logger.info("generate API token complete: {}", hfsAction);

        return hfsAction.responsePayload;
    }


    private String makeCallOutToCPanelVertical(long vmId, Instant timeoutAt) throws Exception {
        CPanelAction hfsAction;
        Long hfsActionId = cache.get(vmId);
        if (hfsActionId == null)
        {
            logger.info("Sending API token generation request to HFS for vm: {}", vmId);
            hfsAction = this.cPanelService.requestApiToken(vmId);
            cache.put(vmId, hfsAction.actionId);
        } else {
            hfsAction = cPanelService.getAction(hfsActionId);

            logger.info("Found api token action {} in progress for vm : {}", hfsActionId, vmId);
        }
        return pollForHfsAction(hfsAction, timeoutAt, vmId);
    }

    private String getApiTokenFromHFS(long vmId, Instant timeoutAt) throws Exception {
        logger.info("sending HFS request to access cPanel VM (generate API token) for vmId {}", vmId);

        String payload = makeCallOutToCPanelVertical(vmId, timeoutAt);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(payload);
        String apiToken = (String) jsonObject.get("api_token");
        return apiToken;
    }
}
