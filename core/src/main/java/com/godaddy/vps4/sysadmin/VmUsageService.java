package com.godaddy.vps4.sysadmin;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.util.TimestampUtils;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class VmUsageService {

    private static final Logger logger = LoggerFactory.getLogger(VmUsageService.class);
    final SysAdminService sysAdminService;
    final Cache<Long, VmUsage> cache;

    @Inject
    public VmUsageService(SysAdminService sysAdminService, CacheManager cacheManager) {
        this.sysAdminService = sysAdminService;
        this.cache = cacheManager.getCache(CacheName.VM_USAGE, Long.class, VmUsage.class);
    }

    public VmUsage getUsage(Long hfsVmId) throws java.text.ParseException {
        VmUsage usage = cache.get(hfsVmId);
        if (usage == null)
            usage = new VmUsage();

        if (usage.isRefreshInProgress()) {
            SysAdminAction updateAction = sysAdminService.getSysAdminAction(usage.pendingHfsActionId);
            if (updateAction.status == SysAdminAction.Status.COMPLETE || updateAction.status == SysAdminAction.Status.FAILED) {
                usage.markRefreshCompleted(TimestampUtils.parseHfsTimestamp(updateAction.completedAt));
                usage.updateUsageStats(fetchUsageStatsFromHfs(hfsVmId));
            }
        } else if (usage.canRefresh()) {
            SysAdminAction updateAction = sysAdminService.usageStatsUpdate(hfsVmId, 0);
            usage.pendingHfsActionId = updateAction.sysAdminActionId;
        }

        cache.put(hfsVmId, usage);
        return usage;
    }

    JSONObject fetchUsageStatsFromHfs(long hfsVmId) throws java.text.ParseException {
        Response response = sysAdminService.usageStatsResults(hfsVmId, null, null);

        if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
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
                return jsonObject;

            } catch (ParseException e) {
                throw new java.text.ParseException("Unable to parse usage", 0);
            }
        } else {
            logger.warn("Bad response for server usage for HFS vm {}: status {}",
                    hfsVmId, response.getStatus());
        }
        return null;
    }
}
