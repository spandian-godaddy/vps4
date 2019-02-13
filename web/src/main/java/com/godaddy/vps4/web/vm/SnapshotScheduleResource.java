package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import org.json.simple.JSONObject;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    private final Config config;

    public static class ScheduleSnapshotRequest {

        @ApiModelProperty(
                value = "This is an ISO 8601 formatted date string",
                example = "2018-01-24T16:52:55Z",
                dataType = "java.lang.String")
        public Instant snapshotTime;

        @ApiModelProperty(
                value = "This is an ISO 8601 formatted date string",
                example = "2018-01-24T16:52:55Z",
                dataType = "java.lang.String")
        public Instant windowStartTime;

        @ApiModelProperty(
                value = "This is an ISO 8601 formatted date string",
                example = "2018-01-24T16:52:55Z",
                dataType = "java.lang.String")
        public Instant windowEndTime;

        private Instant requestedTime;

        public void validate(){
            if(snapshotTime != null){
                if(snapshotTime.isBefore(Instant.now())){
                    throw new Vps4Exception("INVALID_SNAPSHOT_TIME", "Snapshot time must be in the future");
                }
                return;
            }
            else if(windowStartTime != null && windowEndTime != null){
                if(!windowEndTime.isAfter(windowStartTime)){
                    throw new Vps4Exception("INVALID_SNAPSHOT_WINDOW_TIME", "Window end time must be after window start time");
                }
                else if(windowStartTime.isBefore(Instant.now())){
                    throw new Vps4Exception("INVALID_SNAPSHOT_WINDOW_TIME", "Window start time must be in the future");
                }
                return;
            }
            throw new Vps4Exception("NO_TIME_SPECIFIED", "Snapshot time or window start and end times must be set");
        }

        private void setRequestedTime(){
            if(snapshotTime != null){
                requestedTime = snapshotTime;
            }
            else {
                Date randomDate = new Date(ThreadLocalRandom.current()
                        .nextLong(Date.from(windowStartTime).getTime(), Date.from(windowEndTime).getTime()));
                requestedTime = Instant.ofEpochMilli(randomDate.getTime());
            }
        }

        public Instant getSnapshotTime(){
            if(requestedTime == null){
                setRequestedTime();
            }
            return requestedTime;

        }
    }

    @Inject
    public SnapshotScheduleResource(GDUser user,
                                    CreditService creditService,
                                    VirtualMachineService virtualMachineService,
                                    SchedulerWebService schedulerWebService,
                                    ScheduledJobService scheduledJobService,
                                    ActionService actionService,
                                    Config config
                              ) {
        this.user = user;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
        this.schedulerWebService = schedulerWebService;
        this.scheduledJobService = scheduledJobService;
        this.actionService = actionService;
        this.config = config;
    }

    private VirtualMachine getVirtualMachine(UUID vmId) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vmId, virtualMachine, user);
        if (user.isShopper()) {
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
        }
        return virtualMachine;
    }

    @POST
    @Path("/{vmId}/snapshotSchedules/{scheduleId}/pause")
    public SnapshotSchedule newPauseAutomaicSnapshots(@PathParam("vmId") UUID vmId, @PathParam("scheduleId") UUID scheduleId) {
        VirtualMachine virtualMachine = getVirtualMachine(vmId);
        if(virtualMachine.backupJobId == null){
            throw new Vps4Exception("INVALID_BACKUP_JOB_ID", "No automatic backup job assigned to this vm.");
        }
        if(!virtualMachine.backupJobId.equals(scheduleId)){
            throw new Vps4Exception("INVALID_BACKUP_JOB_ID", "Can only pause VM's automatic backup schedule.");
        }

        SchedulerJobDetail schedulerJobDetail = schedulerWebService.pauseJob("vps4", "backups", virtualMachine.backupJobId);

        createNewAction(vmId, ActionType.PAUSE_AUTO_SNAPSHOT);

        return new SnapshotSchedule(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, schedulerJobDetail);
    }

    @POST
    @Path("/{vmId}/snapshotSchedules/{scheduleId}/resume")
    public SnapshotSchedule newResumeAutomaticSnapshots(@PathParam("vmId") UUID vmId, @PathParam("scheduleId") UUID scheduleId) {
        VirtualMachine virtualMachine = getVirtualMachine(vmId);
        if(virtualMachine.backupJobId == null){
            throw new Vps4Exception("INVALID_BACKUP_JOB_ID", "No Backup Job assigned to this vm.");
        }
        if(!virtualMachine.backupJobId.equals(scheduleId)){
            throw new Vps4Exception("INVALID_BACKUP_JOB_ID", "Can only resume VM's automatic backup schedule.");
        }

        SchedulerJobDetail schedulerJobDetail = schedulerWebService.resumeJob("vps4", "backups", virtualMachine.backupJobId);

        createNewAction(vmId, ActionType.RESUME_AUTO_SNAPSHOT);
        return new SnapshotSchedule(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, schedulerJobDetail);
    }

    @POST
    @Path("/{vmId}/snapshotSchedules")
    public SnapshotSchedule scheduleSnapshot(@PathParam("vmId") UUID vmId, ScheduleSnapshotRequest request) {
        // make sure the times are correct
        request.validate();
        // make sure the vm belongs to the customer
        getVirtualMachine(vmId);

        if(manualSnapshotScheduleExists(vmId)){
            throw new Vps4Exception("MANUAL_SNAPSHOT_SCHEDULE_EXISTS", "A manual snapshot schedule already exists.");
        }

        // create the job in the scheduler
        Vps4BackupJobRequest jobRequest = createJobRequestData(vmId, request.getSnapshotTime(), JobType.ONE_TIME, null, ScheduledJob.ScheduledJobType.BACKUPS_MANUAL);

        SchedulerJobDetail jobDetail = schedulerWebService.submitJobToGroup("vps4", "backups", jobRequest);

        // record the job in the vps4 jobs table
        scheduledJobService.insertScheduledJob(jobDetail.id, vmId, ScheduledJob.ScheduledJobType.BACKUPS_MANUAL);

        createNewAction(vmId, ActionType.SCHEDULE_MANUAL_SNAPSHOT);

        return new SnapshotSchedule(ScheduledJob.ScheduledJobType.BACKUPS_MANUAL, jobDetail);
    }

    @GET
    @Path("/{vmId}/snapshotSchedules/{scheduleId}")
    public SnapshotSchedule getScheduledJob(@PathParam("vmId") UUID vmId, @PathParam("scheduleId") UUID scheduleId){
        VirtualMachine virtualMachine = getVirtualMachine(vmId);
        if(virtualMachine.backupJobId.equals(scheduleId)){
            // asking for automatic backups job
            SnapshotSchedule snapshotSchedule = new SnapshotSchedule();
            snapshotSchedule.scheduledJobType = ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC;
            snapshotSchedule.schedulerJobDetail = getSchedulerJobDetail(scheduleId);
            return snapshotSchedule;
        }

        // make sure the job exists and that it belongs to this vm.
        ScheduledJob scheduledJob = scheduledJobService.getScheduledJob(scheduleId);
        validateJob(scheduledJob, vmId);

        SnapshotSchedule snapshotSchedule = getSnapshotScheduleOrSync(scheduledJob);
        if(snapshotSchedule == null){
            throw new NotFoundException("Scheduled Job Not Found");
        }
        return snapshotSchedule;
    }

    @GET
    @Path("/{vmId}/snapshotSchedules")
    public List<SnapshotSchedule> getScheduledJobs(@PathParam("vmId") UUID vmId){
        //validate vm credentials
        VirtualMachine virtualMachine = getVirtualMachine(vmId);

        List<SnapshotSchedule> snapshotSchedules = getScheduledJobsForVm(vmId)
                .stream()
                .map(job -> getSnapshotScheduleOrSync(job))
                .filter(job -> job != null)
                .collect(Collectors.toList());

        // add the automatic backup job to the list.
        if(virtualMachine.backupJobId != null) {
            try {
                SchedulerJobDetail automaticSchedulerJobDetail = getSchedulerJobDetail(virtualMachine.backupJobId);
                snapshotSchedules.add(new SnapshotSchedule(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, automaticSchedulerJobDetail));
            }catch(Exception e){

            }
        }
        return snapshotSchedules;
    }

    private boolean manualSnapshotScheduleExists(UUID vmId){
        return !getManualBackupJobsForVm(vmId)
                .stream()
                .map(job -> getSnapshotScheduleOrSync(job))
                .filter(job -> job != null)
                .collect(Collectors.toList())
                .isEmpty();

    }

    private List<ScheduledJob> getScheduledJobsForVm(UUID vmId) {
        List<ScheduledJob> scheduledJobs = getRetryBackupJobsForVm(vmId);
        scheduledJobs.addAll(getManualBackupJobsForVm(vmId));
        return scheduledJobs;
    }

    private List<ScheduledJob> getRetryBackupJobsForVm(UUID vmId) {
        return scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.BACKUPS_RETRY);
    }

    private List<ScheduledJob> getManualBackupJobsForVm(UUID vmId) {
        return scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.BACKUPS_MANUAL);
    }

    @DELETE
    @Path("/{vmId}/snapshotSchedules/{scheduleId}")
    public void deleteScheduledJob(@PathParam("vmId") UUID vmId, @PathParam("scheduleId") UUID scheduleId){
        // This will work if the job is listed in the vps4 database, but if there is a job in the scheduler but not
        // in the vps4 database, this will not delete it.  It is too difficult to validate the job belongs to the customer
        // if it is only in the scheduler database.

        VirtualMachine virtualMachine = getVirtualMachine(vmId);

        if(scheduleId.equals(virtualMachine.backupJobId)){
            throw new Vps4Exception("CANNOT_DELETE_AUTO_BACKUP_JOB", "Cannot Delete Automatic Backup Job.  Use Pause Instead");
        }

        ScheduledJob scheduledJob = scheduledJobService.getScheduledJob(scheduleId);
        validateJob(scheduledJob, vmId);
        if(scheduledJob.type.equals(ScheduledJob.ScheduledJobType.BACKUPS_RETRY)){
            throw new Vps4Exception("CANNOT_DELETE_RETRY_BACKUP_JOB", "Cannot delete retry backup job using this endpoint");
        }

        try{
            schedulerWebService.deleteJob("vps4", "backups", scheduleId);
        }catch(Exception e){
            // it didn't exist, so don't need to do anything with the scheduler
        }

        scheduledJobService.deleteScheduledJob(scheduleId);

        createNewAction(vmId, ActionType.DELETE_MANUAL_SNAPSHOT_SCHEDULE);
    }

    @PATCH
    @Path("/{vmId}/snapshotSchedules/{scheduleId}")
    public SnapshotSchedule updateScheduledJob(@PathParam("vmId") UUID vmId, @PathParam("scheduleId") UUID scheduleId, ScheduleSnapshotRequest request){
        // validate that the times are correct
        request.validate();
        SnapshotSchedule snapshotSchedule = getScheduledJob(vmId, scheduleId);
        if(snapshotSchedule.scheduledJobType.equals(ScheduledJob.ScheduledJobType.BACKUPS_RETRY)){
            throw new Vps4Exception("CANNOT_RESCHEDULE_RETRY_JOB", "Cannot reschedule retry backup job using this endpoint");
        }
        SchedulerJobDetail schedulerJobDetail = getScheduledJob(vmId, scheduleId).schedulerJobDetail;

        Vps4BackupJobRequest vps4BackupJobRequest = createJobRequestData(vmId,
                request.getSnapshotTime(),
                schedulerJobDetail.jobRequest.jobType,
                schedulerJobDetail.jobRequest.repeatIntervalInDays,
                snapshotSchedule.scheduledJobType);

        schedulerWebService.rescheduleJob("vps4", "backups", scheduleId, vps4BackupJobRequest);

        ActionType actionType = getRescheduleActionType(vmId, scheduleId);
        createNewAction(vmId, actionType);

        return getScheduledJob(vmId, scheduleId);

    }

    private ActionType getRescheduleActionType(UUID vmId, UUID scheduleId) {
        VirtualMachine virtualMachine = getVirtualMachine(vmId);
        ActionType actionType = ActionType.RESCHEDULE_MANUAL_SNAPSHOT;
        if(virtualMachine.backupJobId.equals(scheduleId)){
            actionType = ActionType.RESCHEDULE_AUTO_SNAPSHOT;
        }
        return actionType;
    }

    private long createNewAction(UUID vmId, ActionType scheduleManualSnapshot) {
        // create an action
        long actionId = this.actionService.createAction(vmId,
                scheduleManualSnapshot,
                new JSONObject().toJSONString(),  //TODO Add the scheduled date here.
                user.getUsername());

        // complete the action
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), "");
        return actionId;
    }

    private void validateJob(ScheduledJob vps4Job, UUID vmId){
        if(vps4Job == null){
            throw new NotFoundException("Scheduled Job Not Found");
        }
        if(!vps4Job.vmId.equals(vmId)){
            throw new NotFoundException("Scheduled Job Not Found");
        }
    }

    private SchedulerJobDetail getSchedulerJobDetail(UUID scheduleId) {
        return schedulerWebService.getJob("vps4", "backups", scheduleId);
    }

    private SnapshotSchedule getSnapshotScheduleOrSync(ScheduledJob scheduledJob){
        try{
            SchedulerJobDetail schedulerJobDetail = getSchedulerJobDetail(scheduledJob.id);
            return new SnapshotSchedule(scheduledJob.type, schedulerJobDetail);

        }catch (Exception e){
            scheduledJobService.deleteScheduledJob(scheduledJob.id);
            return null;
        }
    }

    private Vps4BackupJobRequest createJobRequestData(UUID vmId, Instant when,
                                                      JobType jobType, Integer repeatIntervalInDays,
                                                      ScheduledJob.ScheduledJobType scheduledJobType) {
        Vps4BackupJobRequest vps4BackupJobRequest = new Vps4BackupJobRequest();
        vps4BackupJobRequest.vmId = vmId;
        vps4BackupJobRequest.jobType = jobType;
        vps4BackupJobRequest.when = when;
        vps4BackupJobRequest.backupName = config.get("vps4.scheduled.backupName");
        vps4BackupJobRequest.shopperId = user.getShopperId();
        vps4BackupJobRequest.repeatIntervalInDays = repeatIntervalInDays;
        vps4BackupJobRequest.scheduledJobType = scheduledJobType;

        return vps4BackupJobRequest;
    }

    private void deleteSnapshotJobFromScheduler(UUID jobId){
        try{
            schedulerWebService.deleteJob("vps4", "backups", jobId);
        }catch(Exception e){
            // An exception will be thrown if the job does not exist.
        }
        scheduledJobService.deleteScheduledJob(jobId);
    }

}
