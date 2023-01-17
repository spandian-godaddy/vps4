package com.godaddy.vps4.phase2.scheduledJob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ScheduledJobServiceTest {
    
    Injector injector = Guice.createInjector(new DatabaseModule(), new VmModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    ScheduledJobService sjs = injector.getInstance(ScheduledJobService.class);
    VirtualMachine vm1;
    VirtualMachine vm2;
    Vps4User user;

    @Before
    public void setup() {
        user = SqlTestData.insertTestVps4User(dataSource);
        vm1 = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, user.getId());
        vm2 = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, user.getId());
    }
    
    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm1.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm2.vmId, dataSource);
        SqlTestData.deleteTestVps4User(dataSource);
    }
    
    @Test
    public void testInsertScheduledJobs() {
        UUID jobId = UUID.randomUUID();
        sjs.insertScheduledJob(jobId, vm1.vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        ScheduledJob job = sjs.getScheduledJob(jobId);
        
        assertEquals(jobId, job.id);
        assertEquals(vm1.vmId, job.vmId);
        assertEquals(ScheduledJob.ScheduledJobType.ZOMBIE, job.type);
        assertTrue(Instant.now().plusSeconds(120).isAfter(job.created));
    }
    
    @Test
    public void testGetJob() {
        UUID jobId = UUID.randomUUID();
        sjs.insertScheduledJob(jobId, vm1.vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        
        ScheduledJob job = sjs.getScheduledJob(jobId);
        
        assertEquals(jobId, job.id);
        assertEquals(vm1.vmId, job.vmId);
        assertEquals(ScheduledJob.ScheduledJobType.ZOMBIE, job.type);
        assertTrue(Instant.now().plusSeconds(120).isAfter(job.created));
    }
    
    @Test
    public void testGetJobs() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        UUID jobId3 = UUID.randomUUID();
        
        sjs.insertScheduledJob(jobId1, vm1.vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        sjs.insertScheduledJob(jobId2, vm1.vmId, ScheduledJob.ScheduledJobType.BACKUPS_RETRY);
        sjs.insertScheduledJob(jobId3, vm2.vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        
        UUID[] expectedJobs = new UUID[] {jobId1, jobId2};
        
        List<ScheduledJob> jobs = sjs.getScheduledJobs(vm1.vmId);
        
        assertEquals(jobId2, jobs.get(0).id);
        assertEquals(jobId1, jobs.get(1).id);
        
        assertEquals(expectedJobs.length, jobs.size());
    }
    
    @Test
    public void testGetJobsByType() {
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        UUID jobId3 = UUID.randomUUID();
        
        sjs.insertScheduledJob(jobId1, vm1.vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        sjs.insertScheduledJob(jobId2, vm1.vmId, ScheduledJob.ScheduledJobType.BACKUPS_RETRY);
        sjs.insertScheduledJob(jobId3, vm2.vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        
        UUID[] expectedJobs = new UUID[] {jobId1};
        
        List<ScheduledJob> jobs = sjs.getScheduledJobsByType(vm1.vmId, ScheduledJobType.ZOMBIE);
        
        assertTrue(jobs.stream().anyMatch(job -> jobId1.equals(job.id)));
        assertEquals(expectedJobs.length, jobs.size());
    }
}
