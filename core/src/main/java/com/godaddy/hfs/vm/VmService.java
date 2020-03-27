package com.godaddy.hfs.vm;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.backupstorage.BackupStorage;
import com.godaddy.hfs.backupstorage.BackupStorageCreds;

import io.swagger.annotations.Api;

@Path("/api/v1/vms")
@Api(tags={"vm"})

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface VmService {


    @GET
    @Path("{vmId}/")
    Vm getVm(@PathParam("vmId") long vmId);

    @GET
    @Path("{vmId}/actions/{vmActionId}/")
    VmAction getVmAction(@PathParam("vmId") long vmId, @PathParam("vmActionId") long vmActionId);

    @GET
    @Path("/")
    VmList listVms( @QueryParam("sgid")  String sgid);

    @POST
    @Path("/")
    VmAction createVm(CreateVMRequest request);

    @POST
    @Path("/withFlavor")
    VmAction createVmWithFlavor(CreateVMWithFlavorRequest request);

    @POST
    @Path("/{vmId}/destroy")
    VmAction destroyVm(@PathParam("vmId") long vmId);

    @GET
    @Path("/flavors")
    FlavorList listFlavors();

    @GET
    @Path("/bulk")
    VmList getVmsBulk(@QueryParam("vmIds") String vmIds);

    @POST
    @Path("/{vmId}/start")
    VmAction startVm(@PathParam("vmId") long vmId);

    @POST
    @Path("/{vmId}/stop")
    VmAction stopVm(@PathParam("vmId") long vmId);

    @GET
    @Path("/{vmId}/console")
    Console getConsole(@PathParam("vmId") long vmId);

    @POST
    @Path("/{vmId}/console")
    VmAction createConsoleUrl(@PathParam("vmId") long vmId, ConsoleRequest consoleRequest);

    @POST
    @Path("/{vmId}/reboot")
    VmAction rebootVm(@PathParam("vmId") long vmId);

    @POST
    @Path("/{vmId}/rebuild")
    VmAction rebuildVm(@PathParam("vmId") long vmId, RebuildDedicatedRequest request);

    @POST
    @Path("/{vmId}/rescue")
    VmAction rescueVm(@PathParam("vmId") long vmId);

    @POST
    @Path("/{vmId}/rescue/end")
    VmAction endRescueVm(@PathParam("vmId") long vmId);

    @POST
    @Path("/{vmId}/utilization")
    ServerUsageStats updateServerUsageStats(@PathParam("vmId") long vmId);

    @GET
    @Path("/{vmId}/utilization/{utilizationId}")
    ServerUsageStats getServerUsageStats(@PathParam("vmId") long vmId, @PathParam("utilizationId") long utilizationId);

    @GET
    @Path("/{vmId}/agent")
    AgentDetails getHfsAgentDetails(@PathParam("vmId") long vmId);

    @GET
    @Path("/inventory")
    List<HfsInventoryData> getInventory(@QueryParam("provider") String provider);

    @POST
    @Path("/{vmId}/backupSpace")
    VmAction createBackupStorage(@PathParam("vmId") long vmId);

    @DELETE
    @Path("/{vmId}/backupSpace")
    VmAction destroyBackupStorage(@PathParam("vmId") long vmId);

    @GET
    @Path("/{vmId}/backupSpace")
    BackupStorage getBackupStorage(@PathParam("vmId") long vmId);

    @POST
    @Path("/{vmId}/backupSpace/credentials")
    VmAction resetBackupStorageCreds(@PathParam("vmId") long vmId);

    @GET
    @Path("/{vmId}/backupSpace/credentials")
    BackupStorageCreds getBackupStorageCreds(@PathParam("vmId") long vmId);
}
