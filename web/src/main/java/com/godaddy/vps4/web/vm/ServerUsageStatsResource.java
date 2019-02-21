package com.godaddy.vps4.web.vm;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/v2/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServerUsageStatsResource {

    private final VmResource vmResource;
    private final ServerUsageStatsService serverUsageStatsService;

    @Inject
    public ServerUsageStatsResource(VmResource vmResource, ServerUsageStatsService serverUsageStatsService) {
        this.vmResource = vmResource;
        this.serverUsageStatsService = serverUsageStatsService;
    }

    @GET
    @Path("{vmId}/usage")
    @ApiOperation(value = "Get server usage stats",
            notes = "Get the usage stats for the specified server.")
    public UsageStats getUsage(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        ServerUsageStats serverUsageStats = serverUsageStatsService.getServerUsage(vm.hfsVmId);
        return mapToUsageStats(serverUsageStats);
    }

    private UsageStats mapToUsageStats(ServerUsageStats serverUsageStats) {
        UsageStats stats = new UsageStats();
        stats.lastRefreshedAt = serverUsageStats.getCollected() != null ?
                serverUsageStats.getCollected().toInstant():
                serverUsageStats.getRequested().toInstant();
        stats.status = serverUsageStats.pendingRefresh() ?
                UsageStats.UsageStatsStatus.REQUESTED:
                UsageStats.UsageStatsStatus.UPDATED;
        stats.utilizationId = serverUsageStats.getUtilizationId();

        stats.disk = new UsageStats.DiskUsage();
        stats.disk.diskUsed = serverUsageStats.getDiskUsed();
        stats.disk.diskTotal = serverUsageStats.getDiskTotal();

        stats.cpu = new UsageStats.CpuUsage();
        stats.cpu.cpuUsagePercent = serverUsageStats.getCpuUsed();

        stats.mem = new UsageStats.MemUsage();
        stats.mem.memUsed = serverUsageStats.getMemoryUsed();
        stats.mem.memTotal = serverUsageStats.getMemoryTotal();

        return stats;
    }
}
