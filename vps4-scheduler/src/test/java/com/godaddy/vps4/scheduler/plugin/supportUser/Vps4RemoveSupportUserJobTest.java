package com.godaddy.vps4.scheduler.plugin.supportUser;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.ws.rs.WebApplicationException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.godaddy.vps4.scheduler.api.plugin.Vps4RemoveSupportUserJobRequest;
import com.godaddy.vps4.web.client.VmSupportUserService;
import com.godaddy.vps4.web.vm.VmAction;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;


public class Vps4RemoveSupportUserJobTest {
    static Injector injector;
    static VmSupportUserService mockVmSupportUserService;
    private final JobExecutionContext context = mock(JobExecutionContext.class);

    @Inject
    Vps4RemoveSupportUserJob vps4RemoveSupportUserJob;

    @BeforeClass
    public static void newInjector() {
        mockVmSupportUserService = mock(VmSupportUserService.class);
        injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(VmSupportUserService.class).toInstance(mockVmSupportUserService);
                    }
                }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        initJobRequest();
    }

    @After
    public void tearDown() throws Exception {
    }

    private void initJobRequest() {
        Vps4RemoveSupportUserJobRequest request = new Vps4RemoveSupportUserJobRequest();
        request.vmId = UUID.randomUUID();
        vps4RemoveSupportUserJob.setRequest(request);
    }

    @Test
    public void callsRemoveSupportUser() {
        VmAction action = new VmAction();
        action.id = 1234;
        when(mockVmSupportUserService.removeSupportUser(eq(vps4RemoveSupportUserJob.request.vmId))).thenReturn(action);

        try {
            vps4RemoveSupportUserJob.execute(context);

            verify(mockVmSupportUserService, times(1)).removeSupportUser(eq(vps4RemoveSupportUserJob.request.vmId));

        }
        catch (JobExecutionException e) {
            fail("This shouldn't happen!!");
        }
    }

    @Test(expected = JobExecutionException.class)
    public void throwsJobExecutionExceptionInCaseOfErrorRemovingSupportUser() throws JobExecutionException {
        when(mockVmSupportUserService.removeSupportUser(eq(vps4RemoveSupportUserJob.request.vmId)))
           .thenThrow(new WebApplicationException("Boom!!"));

        vps4RemoveSupportUserJob.execute(context);
    }
}