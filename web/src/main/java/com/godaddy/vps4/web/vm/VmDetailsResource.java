package com.godaddy.vps4.web.vm;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmDetailsResource {

    private final VmResource vmResource;
    private final CreditService creditService;
    private final SchedulerWebService schedulerWebService;
    private final PanoptaDataService panoptaDataService;
    private final VmZombieResource vmZombieResource;
    private final GDUser user;

    @Inject
    public VmDetailsResource(VmResource vmResource, CreditService creditService,
            SchedulerWebService schedulerWebService, PanoptaDataService panoptaDataService,
            VmZombieResource vmZombieResource, GDUser user) {
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.schedulerWebService = schedulerWebService;
        this.panoptaDataService = panoptaDataService;
        this.vmZombieResource = vmZombieResource;
        this.user = user;
    }

    @GET
    @Path("/{vmId}/details")
    public VirtualMachineDetails getVirtualMachineDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        Vm vm = vmResource.getVmFromVmVertical(virtualMachine.hfsVmId);
        return new VirtualMachineDetails(vm);
    }

    @GET
    @Path("/{vmId}/hfsDetails")
    public Vm getMoreDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        return vmResource.getVmFromVmVertical(virtualMachine.hfsVmId);
    }

    @GET
    @Path("/{vmId}/withDetails")
    public VirtualMachineWithDetails getVirtualMachineWithDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        Vm vm = vmResource.getVmFromVmVertical(virtualMachine.hfsVmId);
        String hypervisorHostname = null;
        if (user.isEmployee() && !credit.isDed4()) {
            VmExtendedInfo vmExtendedInfo = vmResource.getVmExtendedInfoFromVmVertical(virtualMachine.hfsVmId);
            hypervisorHostname = vmExtendedInfo.extended.hypervisorHostname;
        }

        PanoptaServerDetails panoptaDetails = panoptaDataService.getPanoptaServerDetails(vmId);

        AutomaticSnapshotSchedule automaticSnapshotSchedule = new AutomaticSnapshotSchedule();
        if (virtualMachine.backupJobId != null) {
            SchedulerJobDetail job = schedulerWebService.getJob("vps4", "backups", virtualMachine.backupJobId);
            if (job != null) {
                Instant nextRun = job.nextRun;
                int repeatIntervalInDays = job.jobRequest.repeatIntervalInDays;
                int copiesToRetain = 1;
                boolean isPaused = job.isPaused;
                automaticSnapshotSchedule =
                        new AutomaticSnapshotSchedule(nextRun, copiesToRetain, repeatIntervalInDays, isPaused);
            }
        }
        List<SchedulerJobDetail> zombieCleanupDetailJobs = Collections.emptyList();
        if (isZombie(virtualMachine)) {
            zombieCleanupDetailJobs = vmZombieResource.getScheduledZombieVmDelete(vmId);
        }
        List<ScheduledZombieCleanupJob> scheduledZombieCleanupJobs = new ArrayList<>();
        zombieCleanupDetailJobs.forEach(job -> {
            ScheduledZombieCleanupJob scheduledZombieCleanupJob = new ScheduledZombieCleanupJob();
            scheduledZombieCleanupJob.jobId = job.id;
            scheduledZombieCleanupJob.nextRun = job.nextRun;
            scheduledZombieCleanupJob.isPaused = job.isPaused;
            scheduledZombieCleanupJobs.add(scheduledZombieCleanupJob);
        });

        return new VirtualMachineWithDetails(virtualMachine, new VirtualMachineDetails(vm), credit.getDataCenter(),
                credit.getShopperId(), automaticSnapshotSchedule, panoptaDetails, scheduledZombieCleanupJobs,
                hypervisorHostname);
    }

    private boolean isZombie(VirtualMachine vm) {
        return vm.canceled.isBefore(Instant.now(Clock.systemUTC()));
    }
}
