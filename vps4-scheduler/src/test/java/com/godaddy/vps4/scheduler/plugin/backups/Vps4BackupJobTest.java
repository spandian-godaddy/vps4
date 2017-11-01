package com.godaddy.vps4.scheduler.plugin.backups;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.client.SsoJwtAuth;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.web.client.VmSnapshotService;
import com.godaddy.vps4.web.snapshot.SnapshotAction;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ws.rs.WebApplicationException;
import java.util.UUID;

import static org.junit.Assert.*;

public class Vps4BackupJobTest {
    static Injector injector;
    static VmSnapshotService mockVmSnapshotService;
    private Vps4BackupJobRequest request;
    private final JobExecutionContext context = mock(JobExecutionContext.class);

    @Inject Vps4BackupJob vps4BackupJob;
    @Captor private ArgumentCaptor<VmSnapshotResource.VmSnapshotRequest> vmSnapshotRequestArgumentCaptor;

    @BeforeClass
    public static void newInjector() {
        mockVmSnapshotService = mock(VmSnapshotService.class);
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(VmSnapshotService.class).annotatedWith(SsoJwtAuth.class).toInstance(mockVmSnapshotService);
                }
            }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        initBackupJobRequest();
    }

    @After
    public void tearDown() throws Exception {
    }

    private void initBackupJobRequest() {
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = UUID.randomUUID();
        request.backupName = "foobar";
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

    @Test(expected = JobExecutionException.class)
    public void throwsJobExecutionExceptionInCaseOfErrorWhileCreatingSnapshot() throws JobExecutionException {
        SnapshotAction action = new SnapshotAction();
        action.snapshotId = UUID.randomUUID();
        when(mockVmSnapshotService.createSnapshot(
                eq(vps4BackupJob.request.vmId),
                any(VmSnapshotResource.VmSnapshotRequest.class)))
            .thenThrow(new WebApplicationException("Boom!!"));

        vps4BackupJob.execute(context);
    }
}