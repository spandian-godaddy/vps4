package com.godaddy.vps4.sysadmin;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.util.TimeStamp;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class VmUsageService {

    private static final Logger logger = LoggerFactory.getLogger(VmUsageService.class);

    final SysAdminService sysAdminService;

    final Cache<Long, CachedVmUsage> cache;

    final int secondsToWaitForHfsAction;

    @Inject
    public VmUsageService(Config config, SysAdminService sysAdminService, CacheManager cacheManager) {
        this.sysAdminService = sysAdminService;
        this.cache = cacheManager.getCache(CacheName.VM_USAGE, Long.class, CachedVmUsage.class);
        this.secondsToWaitForHfsAction = Integer.parseInt(config.get("hfs.sysadmin.secondsToWaitForStatsUpdate"));
    }

    public VmUsage getUsage(long hfsVmId) throws java.text.ParseException {

        // usage has three possible storage locations:
        // 1. VPS4 cache
        // 2. HFS cache
        // 3. HFS uncached (stored on VM, but not pulled off to off-VM storage)
        //
        // possible scenarios
        // 1. VPS4 has cached usage, but it's expired
        //     => call HFS for usage
        // 2. HFS has cached usage, but it's expired
        //    OR
        // 3. usage has never been run for a VM
        //     => post to /usageStatsUpdate with daysToRetrieve=1,
        //        then return the cached usage
        //    (in either case, put something in our cache indicating whether
        //     or not we've already sent the 'update' request to HFS, so we
        //     don't hammer HFS with update requests)
        //    (consequently, when we have an expired VPS4-cached usage and
        //     hit HFS and the initial GET is an updated usage,
        //

        CachedVmUsage cachedUsage = cache.get(hfsVmId);
        if (cachedUsage == null
            || shouldRefresh(cachedUsage.usage)) {

            logger.debug("VPS4 cache empty (hfsVmId={}), replenishing from HFS", hfsVmId);
            VmUsage usage = fetchUsageFromHfs(hfsVmId);

            if (shouldRefresh(usage)) {

                if (shouldSendUpdateRequest(cachedUsage)) {
                    logger.debug("HFS data is old or missing (hfsVmId={}), requesting refresh", hfsVmId);
                    SysAdminAction updateAction = sysAdminService.usageStatsUpdate(hfsVmId, 1);
                    logger.info("updating usage stats with action: {}", updateAction);

                    // update our cache to show that we've requested
                    cachedUsage = new CachedVmUsage(usage, updateAction.sysAdminActionId);
                    cache.put(hfsVmId, cachedUsage);
                }

            } else {
                logger.debug("HFS responded with newer data (hfsVmId={}), caching", hfsVmId);
                cachedUsage = new CachedVmUsage(usage, -1);
                cache.put(hfsVmId, cachedUsage);
            }
        }
        VmUsage usage = cachedUsage.usage;
        if (usage == null) {
            usage = new VmUsage();
        }
        return usage;
    }

    /**
     * If our usage data is stale, determine if we need to make a request to HFS
     * to update the usage data
     *
     * @param cachedUsage
     * @return
     */
    boolean shouldSendUpdateRequest(CachedVmUsage cachedUsage) {

        if (cachedUsage == null) {
            // we have no cached usage, trigger update
            return true;

        }
        else if (cachedUsage.updateActionId <= 0) {
            // we have no active request to HFS, so go ahead and create a new request
            return true;

        } else if (cachedUsage.updateActionId > 0) {
            // the data needs to be refreshed, but we've already sent a request to HFS.
            // Check on the request to see if we need to send another one
            SysAdminAction updateAction = sysAdminService.getSysAdminAction(cachedUsage.updateActionId);

            Instant createdAt = ZonedDateTime.parse(updateAction.createdAt, TimeStamp.hfsActionTimestampFormat).toInstant();

            if (updateAction.status == SysAdminAction.Status.COMPLETE
                    || updateAction.status == SysAdminAction.Status.FAILED
                    || Instant.now().isAfter(createdAt.plusSeconds(secondsToWaitForHfsAction))) {
                // if the action we sent has completed, but we still aren't getting updated data,
                // then send another request. Also if it has been more than 5 minutes since the last request was sent.
                return true;

            }
            else {
                // otherwise, assume there are issues on the HFS side that are keeping
                // the sysadmin requests from completing, and don't flood them with requests
                // (wait for the existing action to go through to a terminal state)
            }
        }
        return false;
    }

    boolean shouldRefresh(VmUsage usage) {

        Instant oldestAccepted = Instant.now().minus(Duration.ofHours(12));

        return usage == null
            || usage.cpu == null
            || usage.cpu.timestamp.isBefore(oldestAccepted)

            || usage.disk == null
            || usage.disk.timestamp.isBefore(oldestAccepted)

            || usage.io == null
            || usage.io.timestamp.isBefore(oldestAccepted)

            || usage.mem == null
            || usage.mem.timestamp.isBefore(oldestAccepted)
            ;
    }

    VmUsage fetchUsageFromHfs(long hfsVmId) throws java.text.ParseException {

        Response response = sysAdminService.usageStatsResults(hfsVmId, null, null);

        // TODO HFS is responding with a 202, but it's giving us an immediate response
        //      without any background work.
        //      This may have been a holdover from the previous usage stats behavior.
        //
        if (response.getStatus() == 202) {

            String json = response.readEntity(String.class);

            try {
                JSONObject jsonObject = (JSONObject)new JSONParser().parse(json);
                Object data = jsonObject.get("data");
                if (data != null
                    && data instanceof String
                    && ((String)data).contains("has not completed")) {
                    // usage stats hasn't been run for this VM
                    return null;
                }
                return new VmUsageParser().parse(jsonObject);

            } catch (ParseException e) {
                throw new java.text.ParseException("Unable to parse usage", 0);
            }
        } else {
            logger.warn("Bad response for server usage for HFS vm {}: status {}",
                    hfsVmId, response.getStatus());
        }
        return null;
    }

    public static class CachedVmUsage {
        /**
         * whether VPS4 has already requested a 'usage stats update' from HFS
         * for this cache entry
         */
        public long updateActionId;

        public VmUsage usage;

        public CachedVmUsage() {

        }

        public CachedVmUsage(VmUsage usage, long updateActionId) {
            this.usage = usage;
            this.updateActionId = updateActionId;
        }
    }
}
