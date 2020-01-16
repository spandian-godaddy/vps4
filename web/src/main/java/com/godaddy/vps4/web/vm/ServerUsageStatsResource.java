package com.godaddy.vps4.web.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaGraph;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerUsageStatsService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.util.RequestValidation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/v2/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServerUsageStatsResource {

    private final VmResource vmResource;
    private final ServerUsageStatsService serverUsageStatsService;
    private final VmService vmService;
    private final PanoptaService panoptaService;
    private final PanoptaDataService panoptaDataService;

    @Inject
    public ServerUsageStatsResource(VmResource vmResource,
                                    ServerUsageStatsService serverUsageStatsService,
                                    VmService vmService,
                                    PanoptaService panoptaService,
                                    PanoptaDataService panoptaDataService) {
        this.vmResource = vmResource;
        this.serverUsageStatsService = serverUsageStatsService;
        this.vmService = vmService;
        this.panoptaService = panoptaService;
        this.panoptaDataService = panoptaDataService;
    }

    @GET
    @Path("{vmId}/usage")
    @ApiOperation(value = "Get server usage stats from Panopta or HFS",
            notes = "Get the usage stats for the specified server.")
    public UsageStats getUsage(@PathParam("vmId") UUID vmId) throws PanoptaServiceException {
        VirtualMachine vm = vmResource.getVm(vmId);
        verifyServerIsActive(vm.hfsVmId);

        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        return (detail != null) ? getPanoptaUsage(vmId, vm.spec) : getHfsUsage(vm.hfsVmId);
    }

    void verifyServerIsActive(long hfsVmId) {
        try {
            RequestValidation.validateServerIsActive(vmService.getVm(hfsVmId));
        } catch (Vps4Exception e) {
            throw new Vps4Exception("USAGE_STATS_UNAVAILABLE", "Usage Stats can be collected for active servers.");
        }
    }

    private UsageStats getPanoptaUsage(UUID vmId, ServerSpec spec) throws PanoptaServiceException {
        List<PanoptaGraph> graphs = panoptaService.getUsageGraphs(vmId, "hour");
        if (graphs == null) {
            throw new Vps4Exception("USAGE_STATS_UNAVAILABLE", "Usage stats are unavailable at the moment.");
        }
        return mapPanoptaUsageStats(spec, graphs);
    }

    private UsageStats mapPanoptaUsageStats(ServerSpec spec, List<PanoptaGraph> graphs) {
        UsageStats stats = new UsageStats();
        stats.lastRefreshedAt = Instant.now();
        for (PanoptaGraph graph : graphs) {
            try {
                switch (graph.type) {
                    case CPU:
                        UsageStats.CpuUsage newCpu = new UsageStats.CpuUsage();
                        newCpu.cpuUsagePercent = getLastPercent(graph.values);
                        stats.cpu = newCpu;
                        break;
                    case RAM:
                        int memMib = spec.memoryMib;
                        UsageStats.MemUsage newMem = new UsageStats.MemUsage();
                        newMem.memUsed = (long) (memMib * getLastPercent(graph.values) / 100); // calculate percentage
                        newMem.memTotal = spec.memoryMib;
                        stats.mem = newMem;
                        break;
                    case DISK:
                        int diskMib = spec.diskGib * 1024; // convert from GB to MB
                        UsageStats.DiskUsage newDisk = new UsageStats.DiskUsage();
                        newDisk.diskUsed = (long) (diskMib * getLastPercent(graph.values) / 100); // calculate percentage
                        newDisk.diskTotal = diskMib;
                        stats.disk = newDisk;
                        break;
                }
                Instant instant = getLastInstant(graph.timestamps);
                if (instant.isBefore(stats.lastRefreshedAt)) {
                    stats.lastRefreshedAt = instant;
                }
            } catch (NullPointerException e) {
                // Panopta gave a null value, do nothing
            }
        }
        stats.status = UsageStats.UsageStatsStatus.UPDATED;
        stats.utilizationId = -1;
        return stats;
    }

    private Double getLastPercent(List<Double> list) {
        Double d = list.get(list.size() - 1);
        // Panopta will sometimes return null for the last value. In that case, check the second to last value.
        if (d == null) {
            d = list.get(list.size() - 2);
        }
        return Math.min(d, 100);
    }

    private Instant getLastInstant(List<Instant> list) {
        return list.get(list.size() - 1);
    }

    private UsageStats getHfsUsage(long hfsVmId) {
        ServerUsageStats serverUsageStats = serverUsageStatsService.getServerUsage(hfsVmId);
        if (serverUsageStats == null) {
            throw new Vps4Exception("USAGE_STATS_UNAVAILABLE", "Usage stats are unavailable at the moment.");
        }
        return mapHfsUsageStats(serverUsageStats);
    }

    private UsageStats mapHfsUsageStats(ServerUsageStats serverUsageStats) {
        UsageStats stats = new UsageStats();
        stats.lastRefreshedAt = serverUsageStats.getRequested().toInstant();
        stats.status = serverUsageStats.getCollected() == null ?
                UsageStats.UsageStatsStatus.REQUESTED :
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
