package com.godaddy.vps4.scheduler.plugin.backups;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.client.VmOhBackupService;
import com.godaddy.vps4.web.client.VmService;
import com.godaddy.vps4.web.ohbackup.OhBackupResource;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.web.client.VmSnapshotService;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class Vps4BackupJobTest {
    static Injector injector;
    static VmSnapshotService mockVmSnapshotService;
    static VmOhBackupService mockVmOhBackupService;
    static VmService mockVmService;
    static Config mockConfig;

    private final JobExecutionContext context = mock(JobExecutionContext.class);

    @Inject Vps4BackupJob vps4BackupJob;
    @Captor private ArgumentCaptor<VmSnapshotResource.VmSnapshotRequest> vmSnapshotRequestArgumentCaptor;
    @Captor private ArgumentCaptor<OhBackupResource.OhBackupRequest> ohBackupRequestArgumentCaptor;

    @BeforeClass
    public static void newInjector() {
        mockVmSnapshotService = mock(VmSnapshotService.class);
        mockVmOhBackupService = mock(VmOhBackupService.class);
        mockVmService = mock(VmService.class);
        mockConfig = mock(Config.class);
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(VmSnapshotService.class).toInstance(mockVmSnapshotService);
                    bind(VmOhBackupService.class).toInstance(mockVmOhBackupService);
                    bind(VmService.class).toInstance(mockVmService);
                    bind(Config.class).toInstance(mockConfig);
                }
            }
        );

        VirtualMachine mockVm = mock(VirtualMachine.class);
        mockVm.spec = mock(ServerSpec.class);
        mockVm.spec.serverType = mock(ServerType.class);
        mockVm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        when(mockVmService.getVm(any())).thenReturn(mockVm);
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        initBackupJobRequest();
    }

    private void initBackupJobRequest() {
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = UUID.randomUUID();
        request.backupName = "foobar";
        request.scheduledJobType = ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC;
        vps4BackupJob.setRequest(request);
    }

    @Test
    public void callsVmSnapshotEndpointToCreateAnAutomaticBackup() {
        SnapshotAction action = new SnapshotAction();
        action.snapshotId = UUID.randomUUID();
        when(mockVmSnapshotService.createSnapshot(
                eq(vps4BackupJob.request.vmId),
                any(VmSnapshotResource.VmSnapshotRequest.class)))
            .thenReturn(action);

        try {
            vps4BackupJob.execute(context);

            verify(mockVmSnapshotService, times(1))
                .createSnapshot(eq(vps4BackupJob.request.vmId), vmSnapshotRequestArgumentCaptor.capture());

            VmSnapshotResource.VmSnapshotRequest request = vmSnapshotRequestArgumentCaptor.getValue();
            Assert.assertEquals(SnapshotType.AUTOMATIC, request.snapshotType);
            Assert.assertEquals(vps4BackupJob.request.backupName, request.name);
        }
        catch (JobExecutionException e) {
            fail("This shouldn't happen!!");
        }
    }

    @Test
    public void callsOhSnapshotEndpointToCreateAScheduledBackup() throws JobExecutionException {
        VirtualMachine mockVm = mock(VirtualMachine.class);
        mockVm.spec = mock(ServerSpec.class);
        mockVm.spec.serverType = mock(ServerType.class);
        mockVm.spec.serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = UUID.randomUUID();
        request.backupName = "scheduledOhBackup";
        vps4BackupJob.setRequest(request);
        when(mockVmService.getVm(request.vmId)).thenReturn(mockVm);
        VmAction mockVmAction = mock(VmAction.class);
        when(mockVmOhBackupService.createOhBackup(eq(request.vmId), any())).thenReturn(mockVmAction);

        vps4BackupJob.execute(context);

        verify(mockVmOhBackupService, times(1))
                .createOhBackup(eq(vps4BackupJob.request.vmId), ohBackupRequestArgumentCaptor.capture());

        OhBackupResource.OhBackupRequest ohBackupRequest = ohBackupRequestArgumentCaptor.getValue();;
        Assert.assertEquals(vps4BackupJob.request.backupName, ohBackupRequest.name);
    }

    @Test(expected = JobExecutionException.class)
    public void throwsExInCaseOfWebApplicationExWhileCreatingSnapshot() throws JobExecutionException {
        SnapshotAction action = new SnapshotAction();
        action.snapshotId = UUID.randomUUID();
        when(mockVmSnapshotService.createSnapshot(
                eq(vps4BackupJob.request.vmId),
                any(VmSnapshotResource.VmSnapshotRequest.class)))
            .thenThrow(new RuntimeException("Boom!!"));

        vps4BackupJob.execute(context);
    }

    @Test(expected = JobExecutionException.class)
    public void throwsExInCaseOfNonLimitReachedClientErrorExWhileCreatingSnapshot() throws JobExecutionException {
        SnapshotAction action = new SnapshotAction();
        action.snapshotId = UUID.randomUUID();
        JSONObject json = new JSONObject();
        json.put("id", "SNAPSHOT_OVER_QUOTA");
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(eq(JSONObject.class))).thenReturn(json);
        when(mockResponse.getStatus()).thenReturn(409);
        ClientErrorException mockException = mock(ClientErrorException.class);
        when(mockException.getResponse()).thenReturn(mockResponse);
        when(mockVmSnapshotService.createSnapshot(
                eq(vps4BackupJob.request.vmId),
                any(VmSnapshotResource.VmSnapshotRequest.class)))
                .thenThrow(mockException);
        vps4BackupJob.execute(context);
    }

    @Test(expected = JobExecutionException.class)
    public void throwsExWhenQuartzCannotScheduleJobInCaseOfLimitReached() throws SchedulerException {
        SnapshotAction action = new SnapshotAction();
        action.snapshotId = UUID.randomUUID();
        JSONObject json = new JSONObject();
        json.put("id", "SNAPSHOT_HV_LIMIT_REACHED");
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(eq(JSONObject.class))).thenReturn(json);
        when(mockResponse.getStatus()).thenReturn(409);
        ClientErrorException mockException = mock(ClientErrorException.class);
        when(mockException.getResponse()).thenReturn(mockResponse);
        when(mockVmSnapshotService.createSnapshot(
                eq(vps4BackupJob.request.vmId),
                any(VmSnapshotResource.VmSnapshotRequest.class)))
                .thenThrow(mockException);
        when(mockConfig.get("vps4.autobackup.rescheduleConcurrentBackupWaitMinutes", "20")).thenReturn("20");
        when(mockConfig.get("vps4.autobackup.rescheduleConcurrentBackupWaitDelta", "10")).thenReturn("10");
        JobDetail jobDetail = mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        Scheduler scheduler = mock(Scheduler.class);
        when(context.getScheduler()).thenReturn(scheduler);
        when(scheduler.scheduleJob(any(Trigger.class))).thenThrow(new SchedulerException());

        vps4BackupJob.execute(context);
    }

    @Test
    public void testCallsQuartzScheduleJobInCaseOfLimitReached() throws SchedulerException {
        SnapshotAction action = new SnapshotAction();
        action.snapshotId = UUID.randomUUID();
        JSONObject json = new JSONObject();
        json.put("id", "SNAPSHOT_HV_LIMIT_REACHED");
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(eq(JSONObject.class))).thenReturn(json);
        when(mockResponse.getStatus()).thenReturn(409);
        ClientErrorException mockException = mock(ClientErrorException.class);
        when(mockException.getResponse()).thenReturn(mockResponse);
        when(mockVmSnapshotService.createSnapshot(
                eq(vps4BackupJob.request.vmId),
                any(VmSnapshotResource.VmSnapshotRequest.class)))
                .thenThrow(mockException);
        when(mockConfig.get("vps4.autobackup.rescheduleConcurrentBackupWaitMinutes", "20")).thenReturn("20");
        when(mockConfig.get("vps4.autobackup.rescheduleConcurrentBackupWaitDelta", "10")).thenReturn("10");
        JobDetail jobDetail = mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        Scheduler scheduler = mock(Scheduler.class);
        when(context.getScheduler()).thenReturn(scheduler);
        vps4BackupJob.execute(context);
        verify(scheduler,times(1)).scheduleJob(any(Trigger.class));
    }
}
