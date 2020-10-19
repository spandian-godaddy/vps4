package com.godaddy.vps4.scheduler.plugin.destroyVm;

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

import com.godaddy.vps4.scheduler.api.plugin.Vps4DestroyVmJobRequest;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.client.VmService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;


public class Vps4DestroyVmJobTest {
    static Injector injector;
    static VmService mockVmService;
    private final JobExecutionContext context = mock(JobExecutionContext.class);

    @Inject Vps4DestroyVmJob vps4DestroyVmJob;

    @BeforeClass
    public static void newInjector() {
        mockVmService = mock(VmService.class);
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(VmService.class).toInstance(mockVmService);
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
        Vps4DestroyVmJobRequest request = new Vps4DestroyVmJobRequest();
        request.vmId = UUID.randomUUID();
        vps4DestroyVmJob.setRequest(request);
    }

    @Test
    public void callsDestroyVm() {
        VmAction action = new VmAction();
        action.id = 1234;
        when(mockVmService.destroyVm(eq(vps4DestroyVmJob.request.vmId))).thenReturn(action);

        try {
            vps4DestroyVmJob.execute(context);
            verify(mockVmService, times(1)).destroyVm(eq(vps4DestroyVmJob.request.vmId));
        }
        catch (JobExecutionException e) {
            fail("This shouldn't happen!!");
        }
    }

    @Test(expected = JobExecutionException.class)
    public void throwsJobExecutionExceptionInCaseOfErrorWhileDestroyingVm() throws JobExecutionException {
        when(mockVmService.destroyVm(eq(vps4DestroyVmJob.request.vmId)))
                .thenThrow(new WebApplicationException("Boom!!"));

        vps4DestroyVmJob.execute(context);
    }
}
