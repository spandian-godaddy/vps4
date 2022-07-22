package com.godaddy.vps4.orchestration.ohbackup;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.oh.jobs.OhJobService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.oh.jobs.models.OhJobStatus;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class WaitForOhJobTest {
    @Mock private CommandContext context;
    @Mock private OhJobService ohJobService;

    @Mock private OhJob job;
    @Mock private VirtualMachine vm;

    private WaitForOhJob command;
    private WaitForOhJob.Request request;

    @Before
    public void setUp() {
        setUpMocks();
        setUpRequest();
        when(ohJobService.getJob(vm.vmId, job.id)).thenReturn(job);
        command = new WaitForOhJob(ohJobService);
    }

    private void setUpMocks() {
        job.id = UUID.randomUUID();
        job.status = OhJobStatus.SUCCESS;
        vm.vmId = UUID.randomUUID();
    }

    private void setUpRequest() {
        this.request = new WaitForOhJob.Request();
        request.vmId = vm.vmId;
        request.jobId = job.id;
    }

    @Test
    public void testSuccess() {
        command.execute(context, request);
        verify(ohJobService, times(1)).getJob(vm.vmId, job.id);
    }

    @Test
    public void testError() {
        job.status = OhJobStatus.ERROR;
        try {
            command.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            verify(ohJobService, times(1)).getJob(vm.vmId, job.id);
        }
    }
}
