package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.json.simple.JSONObject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class SnapshotScheduleResource {
    private final GDUser user;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;
    private final SchedulerWebService schedulerWebService;
    private final ScheduledJobService scheduledJobService;
    private final ActionService actionService;

    @Inject
    public SnapshotScheduleResource(GDUser user,
                                    CreditService creditService,
                                    VirtualMachineService virtualMachineService,
                                    SchedulerWebService schedulerWebService,
                                    ScheduledJobService scheduledJobService,
                                    ActionService actionService
                              ) {
        this.user = user;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
        this.schedulerWebService = schedulerWebService;
        this.scheduledJobService = scheduledJobService;
        this.actionService = actionService;
    }

    @POST
    @Path("/{vmId}/pauseAutomaticSnapshots")
    public VmAction pauseAutomaicSnapshots(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, virtualMachine, user);
        if (user.isShopper()) {
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
        }
        if(virtualMachine.backupJobId == null){
            throw new Vps4Exception("INVALID_BACKUP_JOB_ID", "No Backup Job assigned to this vm.");
        }
        long actionId = this.actionService.createAction(vmId,
                ActionType.PAUSE_AUTO_SNAPSHOT,
                new JSONObject().toJSONString(),
                user.getUsername());

        schedulerWebService.pauseJob("vps4", "backups", virtualMachine.backupJobId);
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), "");
        return new VmAction(this.actionService.getAction(actionId), user.isEmployee());
    }

    @POST
    @Path("/{vmId}/resumeAutomaticSnapshots")
    public VmAction resumeAutomaticSnapshots(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, virtualMachine, user);
        if (user.isShopper()) {
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
        }
        if(virtualMachine.backupJobId == null){
            throw new Vps4Exception("INVALID_BACKUP_JOB_ID", "No Backup Job assigned to this vm.");
        }
        long actionId = this.actionService.createAction(vmId,
                ActionType.RESUME_AUTO_SNAPSHOT,
                new JSONObject().toJSONString(),
                user.getUsername());

        schedulerWebService.resumeJob("vps4", "backups", virtualMachine.backupJobId);
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), "");
        return new VmAction(this.actionService.getAction(actionId), user.isEmployee());
    }
}
