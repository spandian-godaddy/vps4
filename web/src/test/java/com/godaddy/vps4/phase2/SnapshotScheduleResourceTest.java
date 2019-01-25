package com.godaddy.vps4.phase2;


import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.SnapshotSchedule;
import com.godaddy.vps4.web.vm.SnapshotScheduleResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class SnapshotScheduleResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject ScheduledJobService scheduledJobService;
    @Inject VirtualMachineService virtualMachineService;
    @Inject ActionService actionService;

    private GDUser user;
    private VirtualMachine testVm;
    private UUID retryScheduleId = UUID.randomUUID();
    private UUID manualScheduleId = UUID.randomUUID();
    private List<UUID> scheduleIds = new ArrayList<>();

    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);

    @Captor private ArgumentCaptor<Vps4BackupJobRequest> vps4BackupJobRequestArgumentCaptor;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(SchedulerWebService.class).toInstance(schedulerWebService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });


    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        createTestVm();

        Instant futureNextRun = Instant.now().plus(24, ChronoUnit.HOURS);

        scheduledJobService.insertScheduledJob(retryScheduleId, testVm.vmId, ScheduledJob.ScheduledJobType.BACKUPS_RETRY);
        scheduleIds.add(retryScheduleId);
        when(schedulerWebService.getJob("vps4", "backups", retryScheduleId)).thenReturn(getScheduledJobDetail(retryScheduleId, futureNextRun));

        scheduledJobService.insertScheduledJob(manualScheduleId, testVm.vmId, ScheduledJob.ScheduledJobType.BACKUPS_MANUAL);
        scheduleIds.add(manualScheduleId);
        when(schedulerWebService.getJob("vps4", "backups", manualScheduleId)).thenReturn(getScheduledJobDetail(manualScheduleId, futureNextRun));

        SchedulerJobDetail automaticScheduledJobDetail = getScheduledJobDetail(testVm.backupJobId, futureNextRun);
        automaticScheduledJobDetail.jobRequest.jobType = JobType.RECURRING;
        automaticScheduledJobDetail.jobRequest.repeatIntervalInDays = 7;
        when(schedulerWebService.getJob("vps4", "backups", testVm.backupJobId)).thenReturn(automaticScheduledJobDetail);
    }

    @After
    public void teardownTest() {
        scheduledJobService.getScheduledJobs(testVm.vmId)
                .stream()
                .forEach(scheduledJob -> scheduledJobService.deleteScheduledJob(scheduledJob.id));
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private void createTestVm() {
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
    }

    private SnapshotScheduleResource getSnapshotScheduleResource() {
        return injector.getInstance(SnapshotScheduleResource.class);
    }

    private SchedulerJobDetail getScheduledJobDetail(UUID id, Instant nextRun){
        JobRequest jobRequest = new JobRequest();
        jobRequest.jobType = JobType.ONE_TIME;
        jobRequest.when = nextRun;
        return new SchedulerJobDetail(id, nextRun, jobRequest, false);
    }

    @Test
    public void testPauseCallsScheduler() {
        getSnapshotScheduleResource().pauseAutomaicSnapshots(testVm.vmId);
        verify(schedulerWebService, times(1)).pauseJob("vps4", "backups", testVm.backupJobId);
    }

    @Test
    public void testResumeCallsScheduler() {
        getSnapshotScheduleResource().resumeAutomaticSnapshots(testVm.vmId);
        verify(schedulerWebService, times(1)).resumeJob("vps4", "backups", testVm.backupJobId);
    }

    @Test
    public void testNewPauseCallsScheduler() {
        getSnapshotScheduleResource().newPauseAutomaicSnapshots(testVm.vmId, testVm.backupJobId);
        verify(schedulerWebService, times(1)).pauseJob("vps4", "backups", testVm.backupJobId);
    }

    @Test
    public void testNewResumeCallsScheduler() {
        getSnapshotScheduleResource().newResumeAutomaticSnapshots(testVm.vmId, testVm.backupJobId);
        verify(schedulerWebService, times(1)).resumeJob("vps4", "backups", testVm.backupJobId);
    }

    @Test
    public void testGetScheduleExists() {
        SnapshotSchedule snapshotSchedule = getSnapshotScheduleResource().getScheduledJob(testVm.vmId, manualScheduleId);
        Assert.assertEquals(ScheduledJob.ScheduledJobType.BACKUPS_MANUAL, snapshotSchedule.scheduledJobType);
    }

    @Test(expected = NotFoundException.class)
    public void testGetScheduleDoesntExist() {
        getSnapshotScheduleResource().getScheduledJob(testVm.vmId, UUID.randomUUID());
    }

    @Test(expected = NotFoundException.class)
    public void testGetScheduleDoesntExistInScheduler() {
        when(schedulerWebService.getJob("vps4", "backups", manualScheduleId)).thenThrow(new WebApplicationException("Not Found"));
        getSnapshotScheduleResource().getScheduledJob(testVm.vmId, manualScheduleId);
    }

    @Test
    public void testGetScheduleDoesntExistInSchedulerRemovesFromJobTable() {
        ScheduledJob scheduledJob = scheduledJobService.getScheduledJob(manualScheduleId);
        Assert.assertNotNull(scheduledJob);
        when(schedulerWebService.getJob("vps4", "backups", manualScheduleId)).thenThrow(new WebApplicationException("Not Found"));
        try {
            getSnapshotScheduleResource().getScheduledJob(testVm.vmId, manualScheduleId);
            Assert.fail("Should have raised a NotFoundException");
        }catch(NotFoundException e){
            // This was expected
        }
        scheduledJob = scheduledJobService.getScheduledJob(manualScheduleId);
        Assert.assertNull(scheduledJob);
    }

    @Test
    public void testGetAutomaticSchedule() {
        SnapshotSchedule snapshotSchedule = getSnapshotScheduleResource().getScheduledJob(testVm.vmId, testVm.backupJobId);
        Assert.assertEquals(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, snapshotSchedule.scheduledJobType);
    }

    @Test
    public void testGetRetrySchedule() {
        SnapshotSchedule snapshotSchedule = getSnapshotScheduleResource().getScheduledJob(testVm.vmId, retryScheduleId);
        Assert.assertEquals(ScheduledJob.ScheduledJobType.BACKUPS_RETRY, snapshotSchedule.scheduledJobType);
    }

    @Test
    public void getScheduleList() {
        List<SnapshotSchedule> snapshotSchedules = getSnapshotScheduleResource().getScheduledJobs(testVm.vmId);
        Assert.assertEquals(3, snapshotSchedules.size());
        snapshotSchedules.stream().forEach(snapshotSchedule -> Assert.assertNotNull(snapshotSchedule));
    }

    @Test
    public void getScheduleListNoRetryScheduleInSchedulerRemovesFromJobTable() {
        ScheduledJob scheduledJob = scheduledJobService.getScheduledJob(retryScheduleId);
        Assert.assertNotNull(scheduledJob);

        when(schedulerWebService.getJob("vps4", "backups", retryScheduleId)).thenThrow(new WebApplicationException("Not Found"));
        List<SnapshotSchedule> snapshotSchedules = getSnapshotScheduleResource().getScheduledJobs(testVm.vmId);
        Assert.assertEquals(2, snapshotSchedules.size());
        snapshotSchedules.stream().forEach(snapshotSchedule -> Assert.assertTrue(!snapshotSchedule.scheduledJobType.equals(ScheduledJob.ScheduledJobType.BACKUPS_RETRY)));

        scheduledJob = scheduledJobService.getScheduledJob(retryScheduleId);
        Assert.assertNull(scheduledJob);
    }

    @Test
    public void automaticBackupIsIncludedInList() {
        List<SnapshotSchedule> snapshotSchedules = getSnapshotScheduleResource().getScheduledJobs(testVm.vmId);
        SnapshotSchedule automaticSchedule = snapshotSchedules.get(2);
        Assert.assertEquals(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, automaticSchedule.scheduledJobType);
    }

    @Test
    public void handlesNoAutomaticBackup() {
        virtualMachineService.setBackupJobId(testVm.vmId, null);
        List<SnapshotSchedule> snapshotSchedules = getSnapshotScheduleResource().getScheduledJobs(testVm.vmId);
        Assert.assertEquals(2, snapshotSchedules.size());
        snapshotSchedules.stream()
                .forEach(snapshotSchedule -> Assert.assertTrue(!snapshotSchedule.scheduledJobType.equals(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC)));
    }

    @Test
    public void createManualBackupSchedule() {
        when(schedulerWebService.getJob("vps4", "backups", manualScheduleId)).thenThrow(new WebApplicationException("Not Found"));
        Assert.assertNull(getAllActionsOfType(ActionType.SCHEDULE_MANUAL_SNAPSHOT));

        Instant fakeBackupTime = Instant.now().plus(24, ChronoUnit.HOURS);
        SchedulerJobDetail mockNewDetail = getScheduledJobDetail(UUID.randomUUID(), fakeBackupTime);
        when(schedulerWebService.submitJobToGroup(eq("vps4"), eq("backups"), any(Vps4BackupJobRequest.class))).thenReturn(mockNewDetail);
        SnapshotScheduleResource.ScheduleSnapshotRequest request = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        request.snapshotTime = fakeBackupTime;
        SnapshotSchedule newManualSnapshotSchedule = getSnapshotScheduleResource().scheduleSnapshot(testVm.vmId, request);

        Assert.assertEquals(ScheduledJob.ScheduledJobType.BACKUPS_MANUAL, newManualSnapshotSchedule.scheduledJobType);
        Assert.assertNotNull(scheduledJobService.getScheduledJob(newManualSnapshotSchedule.schedulerJobDetail.id));
        Assert.assertEquals(request.snapshotTime, newManualSnapshotSchedule.schedulerJobDetail.nextRun);
        Assert.assertNull(scheduledJobService.getScheduledJob(manualScheduleId));

        verify(schedulerWebService, times(1)).submitJobToGroup(eq("vps4"), eq("backups"), any(Vps4BackupJobRequest.class));
        Assert.assertEquals(1, getAllActionsOfType(ActionType.SCHEDULE_MANUAL_SNAPSHOT).totalRows);

    }

    @Test(expected = Vps4Exception.class)
    public void createManualBackupScheduleRejectsIfScheduleExists() {
        Assert.assertNull(getAllActionsOfType(ActionType.SCHEDULE_MANUAL_SNAPSHOT));

        Instant fakeBackupTime = Instant.now().plus(24, ChronoUnit.HOURS);
        SchedulerJobDetail mockNewDetail = getScheduledJobDetail(UUID.randomUUID(), fakeBackupTime);
        when(schedulerWebService.submitJobToGroup(eq("vps4"), eq("backups"), any(Vps4BackupJobRequest.class))).thenReturn(mockNewDetail);
        SnapshotScheduleResource.ScheduleSnapshotRequest request = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        request.snapshotTime = fakeBackupTime;
        SnapshotSchedule newManualSnapshotSchedule = getSnapshotScheduleResource().scheduleSnapshot(testVm.vmId, request);

    }

    @Test
    public void snapshotRequestPrefersExactTime() {
        Instant snapshotTime = Instant.now();
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.snapshotTime = snapshotTime;
        snapshotRequest.windowStartTime = Instant.now().plus(2, ChronoUnit.HOURS);
        snapshotRequest.windowEndTime = Instant.now().plus(3, ChronoUnit.HOURS);
        Assert.assertEquals(snapshotTime, snapshotRequest.getSnapshotTime());
    }

    @Test
    public void snapshotRequestRandomizesWindowTime() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = Instant.now().plus(2, ChronoUnit.HOURS);
        snapshotRequest.windowEndTime = Instant.now().plus(3, ChronoUnit.HOURS);
        Assert.assertTrue(snapshotRequest.getSnapshotTime().isAfter(snapshotRequest.windowStartTime));
        Assert.assertTrue(snapshotRequest.getSnapshotTime().isBefore(snapshotRequest.windowEndTime));
    }

    @Test
    public void snapshotRequestReturnsTheSameWindowTimeEveryTime() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = Instant.now().plus(2, ChronoUnit.HOURS);
        snapshotRequest.windowEndTime = Instant.now().plus(3, ChronoUnit.HOURS);
        Assert.assertEquals(snapshotRequest.getSnapshotTime(), snapshotRequest.getSnapshotTime());
    }

    @Test
    public void validSnapshotExactTime() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.snapshotTime = Instant.now().plus(1, ChronoUnit.MINUTES);
        snapshotRequest.validate(); // throws Vps4Exception if invalid
    }

    @Test
    public void validSnapshotWindowTime() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = Instant.now().plus(1, ChronoUnit.HOURS);
        snapshotRequest.windowEndTime = Instant.now().plus(3, ChronoUnit.HOURS);
        snapshotRequest.validate(); // throws Vps4Exception if invalid
    }

    @Test(expected = Vps4Exception.class)
    public void invalidSnapshotTimeInPast() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.snapshotTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        snapshotRequest.validate();
    }

    @Test(expected = Vps4Exception.class)
    public void invalidWindowTimesEndBeforeStart() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = Instant.now().plus(3, ChronoUnit.HOURS);
        snapshotRequest.windowEndTime = Instant.now().plus(2, ChronoUnit.HOURS);
        snapshotRequest.validate();
    }

    @Test(expected = Vps4Exception.class)
    public void invalidWindowTimesStartEqualsEnd() {
        Instant snapshotTime = Instant.now().plus(2, ChronoUnit.HOURS);;
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = snapshotTime;
        snapshotRequest.windowEndTime = snapshotTime;
        snapshotRequest.validate();
    }

    @Test(expected = Vps4Exception.class)
    public void invalidWindowTimesNoStartTime() {
        Instant snapshotTime = Instant.now().plus(2, ChronoUnit.HOURS);;
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowEndTime = snapshotTime;
        snapshotRequest.validate();
    }

    @Test(expected = Vps4Exception.class)
    public void invalidWindowTimesNoEndTime() {
        Instant snapshotTime = Instant.now().plus(2, ChronoUnit.HOURS);;
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = snapshotTime;
        snapshotRequest.validate();
    }

    @Test(expected = Vps4Exception.class)
    public void invalidWindowTimesNoStartOrEndTime() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.validate();
    }

    @Test(expected = Vps4Exception.class)
    public void invalidWindowStartTimeInPast() {
        SnapshotScheduleResource.ScheduleSnapshotRequest snapshotRequest = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        snapshotRequest.windowStartTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        snapshotRequest.windowEndTime = Instant.now().plus(1, ChronoUnit.HOURS);
        snapshotRequest.validate();
    }

    private ResultSubset<Action> getAllActionsOfType(ActionType actionType){
        ActionService.ActionListFilters filters = new ActionService.ActionListFilters();
        filters.byVmId(testVm.vmId);
        filters.byType(actionType);
        return actionService.getActionList(filters);
    }

    @Test
    public void deleteManualSchedule() {
        Assert.assertNull(getAllActionsOfType(ActionType.DELETE_MANUAL_SNAPSHOT_SCHEDULE));

        getSnapshotScheduleResource().deleteScheduledJob(testVm.vmId, manualScheduleId);
        Assert.assertNull(scheduledJobService.getScheduledJob(manualScheduleId));
        verify(schedulerWebService, times(1)).deleteJob("vps4", "backups", manualScheduleId);

        Assert.assertEquals(1, getAllActionsOfType(ActionType.DELETE_MANUAL_SNAPSHOT_SCHEDULE).totalRows);
    }

    @Test(expected = Vps4Exception.class)
    public void cannotDeleteAutomaticSnapshots() {
        getSnapshotScheduleResource().deleteScheduledJob(testVm.vmId, testVm.backupJobId);
    }

    @Test(expected = Vps4Exception.class)
    public void cannotDeleteRetrySnapshots() {
        getSnapshotScheduleResource().deleteScheduledJob(testVm.vmId, retryScheduleId);
    }


    @Test
    public void patchScheduledManualJob() {
        Assert.assertNull(getAllActionsOfType(ActionType.RESCHEDULE_MANUAL_SNAPSHOT));

        Instant fakeBackupTime = Instant.now().plus(24, ChronoUnit.HOURS);
        SnapshotScheduleResource.ScheduleSnapshotRequest request = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        request.snapshotTime = fakeBackupTime;

        getSnapshotScheduleResource().updateScheduledJob(testVm.vmId, manualScheduleId, request);
        verify(schedulerWebService, times(1)).rescheduleJob(eq("vps4"), eq("backups"), eq(manualScheduleId), any(Vps4BackupJobRequest.class));

        Assert.assertEquals(1, getAllActionsOfType(ActionType.RESCHEDULE_MANUAL_SNAPSHOT).totalRows);
    }

    @Test
    public void patchScheduledAutomaticJob() {
        Assert.assertNull(getAllActionsOfType(ActionType.RESCHEDULE_AUTO_SNAPSHOT));

        Instant fakeBackupTime = Instant.now().plus(24, ChronoUnit.HOURS);
        SnapshotScheduleResource.ScheduleSnapshotRequest request = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        request.snapshotTime = fakeBackupTime;

        getSnapshotScheduleResource().updateScheduledJob(testVm.vmId, testVm.backupJobId, request);
        verify(schedulerWebService, times(1)).rescheduleJob(eq("vps4"), eq("backups"), eq(testVm.backupJobId), any(Vps4BackupJobRequest.class));

        Assert.assertEquals(1, getAllActionsOfType(ActionType.RESCHEDULE_AUTO_SNAPSHOT).totalRows);
    }

    @Test(expected = Vps4Exception.class)
    public void cannotPatchScheduledRetryJob() {
        Instant fakeBackupTime = Instant.now().plus(24, ChronoUnit.HOURS);
        SnapshotScheduleResource.ScheduleSnapshotRequest request = new SnapshotScheduleResource.ScheduleSnapshotRequest();
        request.snapshotTime = fakeBackupTime;

        getSnapshotScheduleResource().updateScheduledJob(testVm.vmId, retryScheduleId, request);
    }

}
