package com.godaddy.vps4.orchestration.hfs.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.vm.Vps4ReviveZombieVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4ReviveZombieVmTest{
    static Injector injector;
    
    private VirtualMachineService virtualMachineService;
    private ScheduledJobService scheduledJobService;
    private CreditService creditService;
    private CommandContext context;
    private ScheduledJob job;
    private Vps4ReviveZombieVm command;
    private SchedulerWebService schedulerWebService;
    private ActionService actionService;
    
    @Before
    public void setup() {
        virtualMachineService = mock(VirtualMachineService.class);
        schedulerWebService = mock(SchedulerWebService.class);
        scheduledJobService = mock(ScheduledJobService.class);
        creditService = mock(CreditService.class);
        actionService = mock(ActionService.class);
        
        job = new ScheduledJob();
        job.id = UUID.randomUUID();
        job.vmId = UUID.randomUUID();
        job.created = Instant.now();
        job.type = ScheduledJobType.ZOMBIE;
        List<ScheduledJob> jobs = new ArrayList<ScheduledJob>();
        jobs.add(job);
        when(scheduledJobService.getScheduledJobsByType(job.vmId, ScheduledJobType.ZOMBIE)).thenReturn(jobs);
        
        injector = Guice.createInjector(binder -> {
            binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
            binder.bind(ScheduledJobService.class).toInstance(scheduledJobService);
            binder.bind(SchedulerWebService.class).toInstance(schedulerWebService);
            binder.bind(ActionService.class).toInstance(actionService);
        });
        
        command = new Vps4ReviveZombieVm(actionService, virtualMachineService, scheduledJobService, creditService);
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }
    
    @Test
    public void testReviveZombieVm() {
        Vps4ReviveZombieVm.Request request = new Vps4ReviveZombieVm.Request();
        request.vmId = job.vmId;
        request.newCreditId = UUID.randomUUID();
        request.oldCreditId = UUID.randomUUID();
        
        Map<ProductMetaField, String> productMeta = new HashMap<>();
        when(creditService.getProductMeta(request.oldCreditId)).thenReturn(productMeta);

        command.execute(context, request);
        
        String product = Utils.getProductForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        String group = Utils.getJobGroupForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        
        verify(virtualMachineService, times(1)).reviveZombieVm(request.vmId, request.newCreditId);
        verify(schedulerWebService, times(1)).deleteJob(product, group, job.id);
        verify(creditService, times(1)).updateProductMeta(request.newCreditId, productMeta);
    }
}
