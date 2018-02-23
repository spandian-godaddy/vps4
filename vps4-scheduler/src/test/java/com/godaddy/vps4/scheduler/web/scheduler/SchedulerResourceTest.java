package com.godaddy.vps4.scheduler.web.scheduler;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SchedulerResourceTest {

    static Injector injector;
    static SchedulerService mockSchedulerService;

    @Inject private SchedulerResource schedulerResource;

    String product;
    String jobGroup;
    String unRegisteredJobGroup;
    UUID nonExistentJobId;
    List<SchedulerJobDetail> schedulerJobDetailList;
    SchedulerJobDetail createdJobDetail;
    SchedulerJobDetail getJobDetail;
    SchedulerJobDetail updatedJobDetail;

    @BeforeClass
    public static void newInjector() {
        mockSchedulerService = mock(SchedulerService.class);
        injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SchedulerService.class).toInstance(mockSchedulerService);
                    }
                }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        product = "vps4";
        jobGroup = "backups";
        unRegisteredJobGroup = "foobar";
        configMockSchedulerService();
    }

    private void configMockSchedulerService() throws Exception {
        createTestJobDetails();
        createdJobDetail = schedulerJobDetailList.get(0);
        getJobDetail = schedulerJobDetailList.get(0);
        updatedJobDetail = schedulerJobDetailList.get(0);
        nonExistentJobId = UUID.randomUUID();

        when(mockSchedulerService.getGroupJobs(eq(product), eq(jobGroup))).thenReturn(schedulerJobDetailList);
        when(mockSchedulerService.createJob(eq(product), eq(jobGroup), any(String.class)))
            .thenReturn(createdJobDetail);
        when(mockSchedulerService.getJob(eq(product), eq(jobGroup), eq(getJobDetail.id)))
                .thenReturn(getJobDetail);
        when(mockSchedulerService
            .updateJobSchedule(eq(product), eq(jobGroup), eq(updatedJobDetail.id), any(String.class)))
            .thenReturn(createdJobDetail);

        // Exception tests
        when(mockSchedulerService.getJob(eq(product), eq(jobGroup), eq(nonExistentJobId)))
            .thenThrow(new Exception("not found"));
        when(mockSchedulerService.createJob(eq(product), eq(unRegisteredJobGroup), any(String.class)))
            .thenThrow(new Exception("Error"));
        when(mockSchedulerService
            .updateJobSchedule(eq(product), eq(jobGroup), eq(nonExistentJobId), any(String.class)))
            .thenThrow(new Exception("Error"));
    }

    private void createTestJobDetails() {
        schedulerJobDetailList = new ArrayList<>();
        IntStream
            .range(1, 10)
            .forEach(i -> {
                schedulerJobDetailList.add(new SchedulerJobDetail(UUID.randomUUID(), Instant.now().plusSeconds(15), new JobRequest()));
            });
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getGroupJobs() throws Exception {
        List<SchedulerJobDetail> jobDetailList = schedulerResource.getGroupJobs(product, jobGroup);
        verify(mockSchedulerService, times(1)).getGroupJobs(eq(product), eq(jobGroup));
        assertThat(jobDetailList, containsInAnyOrder(schedulerJobDetailList.toArray()));
    }

    @Test
    public void submitJobToGroup() throws Exception {
        String requestJson = "";
        SchedulerJobDetail jobDetail = schedulerResource.submitJobToGroup(product, jobGroup, requestJson);
        verify(mockSchedulerService, times(1))
            .createJob(eq(product), eq(jobGroup), eq(requestJson));
        Assert.assertEquals(jobDetail, createdJobDetail);
    }

    @Test(expected = Exception.class)
    public void submitJobToUnRegisteredGroupThrowsException() throws Exception {
        String requestJson = "";
        schedulerResource.submitJobToGroup(product, unRegisteredJobGroup, requestJson);
        verify(mockSchedulerService, times(1))
                .createJob(eq(product), eq(unRegisteredJobGroup), eq(requestJson));
    }

    @Test
    public void getJob() throws Exception {
        SchedulerJobDetail jobDetail = schedulerResource.getJob(product, jobGroup, getJobDetail.id);
        verify(mockSchedulerService, times(1))
            .getJob(eq(product), eq(jobGroup), eq(getJobDetail.id));
        Assert.assertEquals(jobDetail, getJobDetail);
    }

    @Test(expected = Exception.class)
    public void getNonExistentJobThrowsException() throws Exception {
        schedulerResource.getJob(product, jobGroup, nonExistentJobId);
        verify(mockSchedulerService, times(1))
                .getJob(eq(product), eq(jobGroup), eq(nonExistentJobId));
    }

    @Test
    public void rescheduleJob() throws Exception {
        String requestJson = "";
        SchedulerJobDetail jobDetail
            = schedulerResource.rescheduleJob(product, jobGroup, updatedJobDetail.id, requestJson);
        verify(mockSchedulerService, times(1))
            .updateJobSchedule(eq(product), eq(jobGroup), eq(updatedJobDetail.id), eq(requestJson));
        Assert.assertEquals(jobDetail, updatedJobDetail);
    }

    @Test(expected = Exception.class)
    public void rescheduleNonExistentJobThrowsException() throws Exception {
        String requestJson = "";
    schedulerResource.rescheduleJob(product, jobGroup, nonExistentJobId, requestJson);
        verify(mockSchedulerService, times(1))
                .updateJobSchedule(eq(product), eq(jobGroup), eq(nonExistentJobId), eq(requestJson));
    }

    @Test
    public void deleteJob() throws Exception {
        schedulerResource.deleteJob(product, jobGroup, getJobDetail.id);
        verify(mockSchedulerService, times(1))
                .deleteJob(eq(product), eq(jobGroup), eq(getJobDetail.id));
    }
}
