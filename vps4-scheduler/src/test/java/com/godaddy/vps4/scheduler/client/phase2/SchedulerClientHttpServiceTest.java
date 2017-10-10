package com.godaddy.vps4.scheduler.client.phase2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.scheduler.client.SchedulerClientService;
import com.godaddy.vps4.scheduler.client.SchedulerHttpClientModule;
import com.godaddy.vps4.scheduler.client.SchedulerResponse;
import com.godaddy.vps4.scheduler.core.config.ConfigModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This is not a unit test. The test methods in this class actually excercise the code and not just the behaviour.
 * When using an IDE, run the Vps4SchedulerMain in a separate run configuration.
 * Run this test class as a separate run configuration.
 * The tests in this class will require the scheduler server to be running.
 * The scheduler server can also be run from the command line and the tests can then be run against the server.
 */
@Ignore
public class SchedulerClientHttpServiceTest {

    private Injector injector;
    private SchedulerClientService.RequestBody body;

    @Before
    public void setUp() throws Exception {

        injector = Guice.createInjector(
                new ConfigModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                    }

                    @Provides
                    protected CloseableHttpClient getHttpClient() {
                        return HttpClients.createDefault();
                    }
                },
                new SchedulerHttpClientModule()
        );
        injector.injectMembers(this);

        body = new SchedulerClientService.RequestBody();
        body.vmId = UUID.randomUUID();
        body.when = Instant.now().plus(8, ChronoUnit.HOURS);

    }

    @After
    public void tearDown() throws Exception {
        // TODO: cleanup test jobs created in the db.
        SchedulerClientService client = injector.getInstance(SchedulerClientService.class);
        List<SchedulerResponse> groupJobs = client.getGroupJobs("vps4", "backups");
        for(SchedulerResponse job : groupJobs) {
            client.deleteJob("vps4","backups", job.getId());
        }
    }

    @Test
    public void submitJobToGroup() throws Exception {
        SchedulerClientService client = injector.getInstance(SchedulerClientService.class);
        SchedulerResponse response = client.submitJobToGroup("vps4", "backups", body);
        assertNotNull(response);
    }

    @Test
    public void getJob() throws Exception {
        SchedulerClientService client = injector.getInstance(SchedulerClientService.class);
        SchedulerResponse response = client.submitJobToGroup("vps4", "backups", body);
        assertNotNull(response);
        UUID jobId = response.getId();

        response = client.getJob("vps4", "backups", jobId);
        assertNotNull(response);
        assertEquals(String.format("Expected job id: %s does not match actual job id: %s.", jobId, response.getId()), jobId, response.getId());
    }

    @Test
    public void rescheduleJob() throws  Exception {
        SchedulerClientService client = injector.getInstance(SchedulerClientService.class);
        SchedulerResponse response = client.submitJobToGroup("vps4", "backups", body);
        assertNotNull(response);
        UUID jobId = response.getId();
        Instant jobScheduledTime = response.getWhen();

        body.setWhen(Instant.now().plus(10, ChronoUnit.HOURS));
        response = client.rescheduleJob("vps4", "backups", jobId, body);
        assertNotNull(response);
        assertEquals(String.format("Expected job id: %s does not match actual job id: %s.", jobId, response.getId()), jobId, response.getId());
        assertTrue("Expected rescheduled job time to be later than original job schedule time.", jobScheduledTime.isBefore(response.getWhen()));
    }

    @Test
    public void getGroupJobs() throws Exception {
        SchedulerClientService client = injector.getInstance(SchedulerClientService.class);
        SchedulerResponse response = client.submitJobToGroup("vps4", "backups", body);
        assertNotNull(response);

        client = injector.getInstance(SchedulerClientService.class);
        List<SchedulerResponse> groupJobs = client.getGroupJobs("vps4", "backups");
        assertNotNull("Response should not be null or empty.", groupJobs);
    }

    @Test
    public void deleteJob() throws Exception {
        SchedulerClientService client = injector.getInstance(SchedulerClientService.class);
        SchedulerResponse response = client.submitJobToGroup("vps4", "backups", body);
        assertNotNull(response);
        UUID jobId = response.getId();

        client = injector.getInstance(SchedulerClientService.class);
        client.deleteJob("vps4", "backups", jobId);

        List<SchedulerResponse> responses = client.getGroupJobs("vps4", "backups");
        assertNotNull(responses);
        SchedulerResponse matchingResponse = responses.stream().filter(responseIterator -> jobId.equals(responseIterator.getId())).findFirst().orElse(null);
        assertNull("Matching job id found. Expected no matches.", matchingResponse);
    }

}