package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.account.Vps4ProcessAccountCancellation;
import com.godaddy.vps4.orchestration.scheduler.RescheduleZombieVmCleanup;
import com.godaddy.vps4.orchestration.vm.Vps4ReviveZombieVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.util.VmHelper;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class VmZombieResource {

    private static final Logger logger = LoggerFactory.getLogger(VmZombieResource.class);

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final CommandService commandService;
    private final GDUser user;
    private final ActionService actionService;
    private final VmActionResource vmActionResource;
    private final SchedulerWebService schedulerWebService;
    private final ScheduledJobService scheduledJobService;
    private final Config config;

    @Inject
    public VmZombieResource(VirtualMachineService virtualMachineService,
            CreditService creditService,
            CommandService commandService,
            GDUser user,
            ActionService actionService,
            VmActionResource vmActionResource,
            SchedulerWebService schedulerWebService,
            ScheduledJobService scheduledJobService,
            Config config) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.commandService = commandService;
        this.user = user;
        this.actionService = actionService;
        this.vmActionResource = vmActionResource;
        this.schedulerWebService = schedulerWebService;
        this.scheduledJobService = scheduledJobService;
        this.config = config;
    }


    @POST
    @Path("/{vmId}/revive")
    @ApiOperation(value = "Revive a zombie vm whose account has been canceled but the server has not yet been deleted",
            notes = "Revive a zombie vm whose account has been canceled but the server has not yet been deleted")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.HS_AGENT, GDUser.Role.SUSPEND_AUTH,
            GDUser.Role.C3_OTHER})
    public VmAction reviveZombieVm(
            @ApiParam(value = "The ID of the server to revive", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "The ID of the new credit to which the VM will be linked",
                    required = true) @QueryParam("newCreditId") UUID newCreditId) {

        logger.info("getting vm with id {}", vmId);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit oldCredit = creditService.getVirtualMachineCredit(vm.orionGuid);
        validateAccountIsRemoved(vmId, oldCredit);

        VirtualMachineCredit newCredit =
                getAndValidateUserAccountCredit(creditService, newCreditId, oldCredit.getShopperId());
        validateCreditIsNotInUse(newCredit);

        validateCreditsMatch(oldCredit, newCredit);

        logger.info("Revive zombie vm: {}", vmId);

        Vps4ReviveZombieVm.Request request = new Vps4ReviveZombieVm.Request();
        request.vmId = vmId;
        request.newCreditId = newCreditId;
        request.oldCreditId = oldCredit.getEntitlementId();

        return VmHelper.createActionAndExecute(actionService, commandService, vm.vmId,
                ActionType.RESTORE_ACCOUNT, request, "Vps4ReviveZombieVm", user);
    }

    @POST
    @Path("/{vmId}/zombie")
    @ApiOperation(value = "Zombie (stop and schedule deletion for) a VM whose account has been canceled",
            notes = "Zombie (stop and schedule deletion for) a VM whose account has been canceled")
    public VmAction zombieVm(
            @ApiParam(value = "The ID of the server to zombie", required = true) @PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        validateAccountIsRemoved(vmId, credit);

        logger.info("Zombie vm: {}", vmId);

        cancelIncompleteVmActions(vmId);

        Vps4ProcessAccountCancellation.Request request = new Vps4ProcessAccountCancellation.Request();
        request.virtualMachineCredit = credit;
        request.initiatedBy = user.getUsername();
        return VmHelper.createActionAndExecute(actionService, commandService, vm.vmId,
                ActionType.CANCEL_ACCOUNT, request, "Vps4ProcessAccountCancellation", user);
    }

    @POST
    @Path("/{vmId}/zombie/reschedule")
    @ApiOperation(value = "Reschedule the deletion of a cancelled VM (vm in zombie status) to after 7 days from now.",
            notes = "Reschedule the deletion of a cancelled VM (vm in zombie status) to after 7 days from now.")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD})
    public void rescheduleZombieVmDelete(
            @ApiParam(value = "Id of the VM in zombie status whose deletion needs to be rescheduled", required = true)
            @PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        validateVmIsZombie(vm);

        List<ScheduledJob> scheduledJobs =
                scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        if (scheduledJobs.size() == 0) {
            logger.error("Expected 1 zombie cleanup job scheduled, returned {}", scheduledJobs.size());
            throw new Vps4Exception("UNEXPECTED_JOB_LIST_SIZE",
                    "Expected 1 zombie cleanup job scheduled, returned " + scheduledJobs.size());
        } else if (scheduledJobs.size() > 1) {
            logger.error("Expected 1 zombie cleanup job scheduled, returned {}. The extras will be deleted", scheduledJobs.size());
            Commands.execute(commandService, "Vps4DeleteExtraScheduledZombieJobsForVm", vmId);
        }

        UUID scheduledJobId = scheduledJobs.get(0).id;
        logger.info("Rescheduling zombie vm clean up job id: {} for vm id {}", scheduledJobId, vmId);
        rescheduleZombieVmCleanupJob(vmId, scheduledJobId);
    }

    @GET
    @Path("/{vmId}/zombie/schedules")
    @ApiOperation(value = "Get scheduled jobs for the deletion of a cancelled VM (vm in zombie status)",
            notes = "Get scheduled jobs for the deletion of a cancelled VM (vm in zombie status)")
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD})
    public List<SchedulerJobDetail> getScheduledZombieVmDelete(
            @ApiParam(value = "Id of the VM in zombie status", required = true)
            @PathParam("vmId") UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, vm, user);

        validateVmIsZombie(vm);

        String product = com.godaddy.vps4.scheduler.api.core.utils.Utils.getProductForJobRequestClass(
                Vps4ZombieCleanupJobRequest.class);
        String jobGroup = com.godaddy.vps4.scheduler.api.core.utils.Utils.getJobGroupForJobRequestClass(
                Vps4ZombieCleanupJobRequest.class);
        List<ScheduledJob> scheduledJobs =
                scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        List<SchedulerJobDetail> schedulerJobDetails = new ArrayList<>();
        scheduledJobs.forEach(scheduledJob -> {
            try{
                SchedulerJobDetail schedulerJobDetail = schedulerWebService.getJob(product, jobGroup, scheduledJob.id);
                schedulerJobDetails.add(schedulerJobDetail);
            }
            catch (Exception e) {
                logger.info("Unable to find scheduler job details for job id {}. " +
                                    "This is expected behavior if the job is already complete", scheduledJob.id);
            }
        });
        return schedulerJobDetails;
    }

    private void rescheduleZombieVmCleanupJob(UUID vmId, UUID jobId) {
        RescheduleZombieVmCleanup.Request rescheduleZombieVmCleanupRequest = new RescheduleZombieVmCleanup.Request();
        rescheduleZombieVmCleanupRequest.vmId = vmId;
        rescheduleZombieVmCleanupRequest.when = calculateValidUntil();
        rescheduleZombieVmCleanupRequest.jobId = jobId;
        Commands.execute(commandService, "RescheduleZombieVmCleanup", rescheduleZombieVmCleanupRequest);
    }

    private Instant calculateValidUntil() {
        int waitTime = Integer.parseInt(config.get("vps4.zombie.cleanup.waittime"));
        return Instant.now().plus(waitTime, ChronoUnit.DAYS);
    }

    private void validateVmIsZombie(VirtualMachine vm) {
        if (!isZombie(vm)) {
            throw new Vps4Exception("UNEXPECTED_VM_STATUS",
                    "Vm status not as expected. Vm should be in zombie status (canceled and scheduled for deletion).");
        }
    }

    private boolean isZombie(VirtualMachine vm) {
        return vm.canceled.isBefore(Instant.now(Clock.systemUTC()));
    }

    private void cancelIncompleteVmActions(UUID vmId) {
        List<Action> actions = actionService.getIncompleteActions(vmId);
        for (Action action : actions) {
            vmActionResource.cancelVmAction(vmId, action.id);
        }
    }

    private void validateCreditsMatch(VirtualMachineCredit oldCredit, VirtualMachineCredit newCredit) {
        if (!oldCredit.getControlPanel().equalsIgnoreCase(newCredit.getControlPanel())) {
            throw new Vps4Exception("CONTROL_PANEL_MISMATCH", "Control panel of old and new credits do not match");
        }
        if (oldCredit.getManagedLevel() != newCredit.getManagedLevel() &&
                !(oldCredit.getManagedLevel()  == 1 && newCredit.getManagedLevel()  == 0 )) {
            throw new Vps4Exception("MANAGED_LEVEL_MISMATCH", "Managed level of the old and new credits do not match");
        }
        if (oldCredit.getMonitoring() != newCredit.getMonitoring() &&
                !(oldCredit.getMonitoring() == 1 && newCredit.getMonitoring() == 0)) {
            throw new Vps4Exception("MONITORING_MISMATCH", "Monitoring of the old and new credits do not match");
        }
        if (!oldCredit.getOperatingSystem().equalsIgnoreCase(newCredit.getOperatingSystem())) {
            throw new Vps4Exception("OPERATING_SYSTEM_MISMATCH",
                    "Operating system of the old and new credits do not match");
        }
        if (oldCredit.getTier() != newCredit.getTier()) {
            throw new Vps4Exception("TIER_MISMATCH", "Tier of the old and new credits do not match");
        }
        if (oldCredit.entitlementData.cdnWaf != newCredit.entitlementData.cdnWaf) {
            throw new Vps4Exception("CDN_MISMATCH", "Cdn addon size of the old and new credits do not match");
        }
    }

    private void validateAccountIsRemoved(UUID vmId, VirtualMachineCredit credit) {
        if (!credit.isAccountRemoved()) {
            throw new Vps4Exception("ACCOUNT_STATUS_NOT_REMOVED",
                    String.format("Cannot revive or zombie %s, account %s status is not removed.", vmId,
                            credit.getEntitlementId()));
        }
    }
}
