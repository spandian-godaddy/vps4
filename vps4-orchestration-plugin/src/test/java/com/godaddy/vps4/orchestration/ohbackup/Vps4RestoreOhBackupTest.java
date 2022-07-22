package com.godaddy.vps4.orchestration.ohbackup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4RestoreOhBackupTest {
    @Mock private ActionService actionService;
    @Mock private CommandContext context;
    @Mock private OhBackupService ohBackupService;

    @Mock private OhBackup backup;
    @Mock private OhJob job;
    @Mock private VirtualMachine vm;

    @Captor private ArgumentCaptor<WaitForOhJob.Request> pollOhActionCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, OhJob>> restoreBackupCaptor;

    private Vps4RestoreOhBackup command;
    private Vps4RestoreOhBackup.Request request;

    @Before
    public void setUp() {
        setUpMocks();
        setUpRequest();
        when(context.execute(eq("RestoreOhBackup"),
                             Matchers.<Function<CommandContext, OhJob>>any(),
                             eq(OhJob.class))).thenReturn(job);
        when(ohBackupService.restoreBackup(vm.vmId, backup.id)).thenReturn(job);
        when(ohBackupService.restoreBackup(any(UUID.class), any(UUID.class))).thenReturn(job);
        command = new Vps4RestoreOhBackup(actionService, ohBackupService);
    }

    private void setUpMocks() {
        vm.vmId = UUID.randomUUID();
        backup.id = UUID.randomUUID();
        job.id = UUID.randomUUID();
        backup.jobId = job.id;
    }

    private void setUpRequest() {
        request = new Vps4RestoreOhBackup.Request();
        request.backupId = backup.id;
        request.virtualMachine = vm;
        request.virtualMachine.vmId = vm.vmId;
    }

    @Test
    public void callsBackupService() {
        command.executeWithAction(context, request);
        verify(context).execute(eq("RestoreOhBackup"), restoreBackupCaptor.capture(), eq(OhJob.class));
        Function<CommandContext, OhJob> lambdaValue = restoreBackupCaptor.getValue();
        OhJob returnValue = lambdaValue.apply(context);
        assertSame(job, returnValue);
    }

    @Test
    public void pollsForBackupToComplete() {
        command.executeWithAction(context, request);
        verify(context).execute(eq(WaitForOhJob.class), pollOhActionCaptor.capture());
        WaitForOhJob.Request result = pollOhActionCaptor.getValue();
        assertEquals(vm.vmId, result.vmId);
        assertEquals(job.id, result.jobId);
    }
}
