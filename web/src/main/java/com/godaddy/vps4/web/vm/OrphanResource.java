package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.Orphans;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.vm.Vm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrphanResource {

    private static final Logger logger = LoggerFactory.getLogger(OrphanResource.class);

    private final VmResource vmResource;
    private final CreditResource creditResource;
    private final VmSnapshotResource snapshotResource;
    private final ProjectService projectService;

    private final NodePingService hfsNodepingService;
    private final NetworkService networkService;

    private final MonitoringMeta monitoringMeta;

    @Inject
    public OrphanResource(VmResource vmResource, CreditResource creditResource, VmSnapshotResource snapshotResource,
                          ProjectService projectService, NodePingService hfsNodepingService,
                          NetworkService networkService, MonitoringMeta monitoringMeta) {
        this.vmResource = vmResource;
        this.creditResource = creditResource;
        this.snapshotResource = snapshotResource;
        this.projectService = projectService;
        this.hfsNodepingService = hfsNodepingService;
        this.networkService = networkService;
        this.monitoringMeta = monitoringMeta;
    }

    @GET
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @Path("/{vmId}/orphanedResources")
    @ApiOperation(value = "Find all resources left in hfs resulting from the failed destroy_vm action",
            notes = "Find all resources left in hfs resulting from the failed destroy_vm action")
    public Orphans getOrphanedResources(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        validateVm(vmId, vm);

        Orphans orphans = new Orphans();
        orphans.controlPanel = vm.image.controlPanel;

        updateOrphanedVm(vm, orphans);

        updateOrphanedSgid(vm, orphans);

        updateOrphanedIp(vm, orphans);

        getOrphanedSnapshots(vmId, orphans);

        return orphans;
    }

    private void validateVm(UUID vmId, VirtualMachine vm) {
        VirtualMachineCredit vmCredit = this.creditResource.getCredit(vm.orionGuid);
        if(vmCredit != null && isVmStillAssignedToCredit(vm, vmCredit)) {
            // this vm is still accounted for in the credit
            logger.warn("The vm " + vmId + " is still accounted for in the " + vm.orionGuid + " credit");
            throw new Vps4Exception("INVALID VM", "Vm " + vmId + " is still assigned to the credit");
        }
    }

    private boolean isVmStillAssignedToCredit(VirtualMachine vm, VirtualMachineCredit vmCredit) {
        return vmCredit.productId != null && vmCredit.productId.equals(vm.vmId);
    }

    private void updateOrphanedVm(VirtualMachine vm, Orphans orphans) {
        orphans.hfsVmId = vm.hfsVmId;

        if (orphans.hfsVmId != 0) {
            //the vm create put something in the database, so we should check it out against hfs
            try {
                Vm hfsVm = vmResource.getVmFromVmVertical(vm.hfsVmId);
                if(hfsVm == null){
                    return;
                }
                orphans.hfsVmStatus = hfsVm.status;
            } catch (Exception e){
                logger.info("could not find hfs vm with id: " + orphans.hfsVmId);
            }
        }
    }

    private void updateOrphanedSgid(VirtualMachine vm, Orphans orphans) {
        Project project = projectService.getProject(vm.projectId);
        if (project != null) {
            orphans.sgid = project.getVhfsSgid();
        }
    }

    private boolean isVirtualMachine(VirtualMachine vm){
        return vm.spec.serverType.serverType.equals(ServerType.Type.VIRTUAL);

    }

    private boolean ipIsValid(IpAddress ip) {
        // Ip is still valid if the validUntil date is after now.
        // Adding 2 days to clear all Timezone issues (valid_until = infinity reads as +292278994-08-16T23:00:00Z)
        return ip.validUntil.isAfter(Instant.now().plus(2, ChronoUnit.DAYS));
    }

    private boolean isHfsIpAssignedToThisVm(String vmSgid, gdg.hfs.vhfs.network.IpAddress hfsIp){
        return vmSgid.equals(hfsIp.sgid);
    }

    private void updateOrphanedIp(VirtualMachine vm, Orphans orphans) {
        IpAddress primaryIp = vm.primaryIpAddress;

        if (primaryIp != null && isVirtualMachine(vm)) {
            try {
                gdg.hfs.vhfs.network.IpAddress hfsIp = networkService.getAddress(primaryIp.ipAddressId);
                logger.debug("Found hfsIp: " + hfsIp);
                if (ipIsValid(primaryIp) && hfsIp != null && isHfsIpAssignedToThisVm(orphans.sgid, hfsIp)) {
                    // our db thinks the IP is still active and hfs
                    // has it assigned to the vm with the failed destroy action
                    orphans.ip = hfsIp;

                }
            } catch (Exception e) {
                logger.info("could not find hfs ip with id: " + primaryIp.ipAddressId);
            }
        }

        updateOrphanedNodepingAccount(primaryIp, orphans);
    }

    private void updateOrphanedNodepingAccount(IpAddress primaryIp, Orphans orphans) {
        if(primaryIp.pingCheckId != null) {
            try {
                NodePingCheck pingCheck = hfsNodepingService.getCheck(monitoringMeta.getAccountId(), primaryIp.pingCheckId);
                if (pingCheck != null &&
                    isPingCheckEnabledInHfs(pingCheck) &&
                    isPingCheckPointingAtThisVmsIp(primaryIp, pingCheck)) {
                    orphans.nodePingCheck = pingCheck;
                }
            } catch (Exception e) {
                logger.info("could not find nodeping check with account id: " + monitoringMeta.getAccountId() +
                        " and ping checkId: " + primaryIp.pingCheckId);
            }

        }
    }

    private boolean isPingCheckPointingAtThisVmsIp(IpAddress primaryIp, NodePingCheck pingCheck) {
        return pingCheck.target.equals(primaryIp.ipAddress);
    }

    private boolean isPingCheckEnabledInHfs(NodePingCheck pingCheck) {
        return "active".equals(pingCheck.enabled);
    }

    private void getOrphanedSnapshots(UUID vmId, Orphans orphans) {
        orphans.snapshotList = snapshotResource.getSnapshotsForVM(vmId);
    }
}
