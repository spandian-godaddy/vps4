package com.godaddy.vps4.web.mailrelay;

import java.util.List;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayHistory;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmMailRelayResource {

    private final MailRelayService mailRelayService;
    private final NetworkService networkService;
    private final PrivilegeService privilegeService;
    private final Vps4User user;
    private final Cache<String, CachedMailRelayUsage> mailRelayUsageCache;
    private final Cache<String, CachedMailRelayHistory> mailRelayHistoryCache;
    
    @Inject
    public VmMailRelayResource(Vps4User user, MailRelayService mailRelayService, NetworkService networkService,
            PrivilegeService privilegeService, CacheManager cacheManager) {
        this.user = user;
        this.mailRelayService = mailRelayService;
        this.networkService = networkService;
        this.privilegeService = privilegeService;
        this.mailRelayUsageCache = cacheManager.getCache(CacheName.MAIL_RELAY_USAGE, String.class, CachedMailRelayUsage.class);
        this.mailRelayHistoryCache = cacheManager.getCache(CacheName.MAIL_RELAY_HISTORY, String.class, CachedMailRelayHistory.class);
    }

    @GET
    @Path("{vmId}/mailRelay/current")
    @ApiOperation(value = "Get today's mail relay use for the selected server", notes = "Get today's mail relay use for the selected server.")
    public MailRelay getCurrentMailRelayUsage(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId, 
            @ApiParam(value = "Force refresh of the mail relay usage cache", required = false) @DefaultValue("false") @QueryParam("force") boolean force) {
        
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        
        CachedMailRelayUsage cachedUsage = mailRelayUsageCache.get(ipAddress.ipAddress);
        if(force || cachedUsage == null){
            MailRelay newUsage = mailRelayService.getMailRelay(ipAddress.ipAddress);
            cachedUsage = new CachedMailRelayUsage(newUsage);
            mailRelayUsageCache.put(ipAddress.ipAddress, cachedUsage);
        }
        return cachedUsage.relayUsage;
    }
    
    @GET
    @Path("{vmId}/mailRelay/history")
    @ApiOperation(value = "Get past mail relay use for the selected server", notes = "Get past mail relay use for the selected server")
    public List<MailRelayHistory> getMailRelayHistory(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "Force refresh of the mail relay history cache", required = true) @DefaultValue("false") @QueryParam("force") boolean force) {

        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        
        CachedMailRelayHistory cachedHistory = mailRelayHistoryCache.get(ipAddress.ipAddress);
        if(force || cachedHistory == null){
            List<MailRelayHistory> newHistory = mailRelayService.getRelayHistory(ipAddress.ipAddress);
            cachedHistory = new CachedMailRelayHistory(newHistory);
            mailRelayHistoryCache.put(ipAddress.ipAddress, cachedHistory);
        }
        return cachedHistory.relayHistory;
    }

    
    public static class CachedMailRelayUsage {
        public MailRelay relayUsage;
        public CachedMailRelayUsage(){
        }
        public CachedMailRelayUsage(MailRelay relayUsage){
            this.relayUsage = relayUsage;
        }
    }
    
    public static class CachedMailRelayHistory {
        public List<MailRelayHistory> relayHistory;
        public CachedMailRelayHistory(){
        }
        public CachedMailRelayHistory(List<MailRelayHistory> relayHistory){
            this.relayHistory = relayHistory;
        }
    }
    
    
}
