package com.godaddy.vps4.orchestration.hfs.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.vm.Vps4ReviveZombieVm;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4ReviveZombieVmTest{
    static Injector injector;

    private VirtualMachineService virtualMachineService;
    private VmService vmService;
    private ScheduledJobService scheduledJobService;
    private CreditService creditService;
    private CommandContext context;
    private ScheduledJob job;
    private Vps4ReviveZombieVm command;
    private SchedulerWebService schedulerWebService;
    private ActionService actionService;
    private VirtualMachine vm;
    private PanoptaDataService panoptaDataService;
    private PanoptaService panoptaService;
    private CdnDataService cdnDataService;
    private CdnService cdnService;
    private VmCdnSite vmCdnSite;
    private CdnDetail cdnDetail;
    private VirtualMachineCredit vmCredit;
    private UUID customerId;

    @Before
    public void setup() throws PanoptaServiceException {
        virtualMachineService = mock(VirtualMachineService.class);
        vmService = mock(VmService.class);
        schedulerWebService = mock(SchedulerWebService.class);
        scheduledJobService = mock(ScheduledJobService.class);
        creditService = mock(CreditService.class);
        actionService = mock(ActionService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        panoptaService = mock(PanoptaService.class);
        cdnDataService = mock(CdnDataService.class);
        cdnService = mock(CdnService.class);

        job = new ScheduledJob();
        job.id = UUID.randomUUID();
        job.vmId = UUID.randomUUID();
        job.created = Instant.now();
        job.type = ScheduledJobType.ZOMBIE;
        List<ScheduledJob> jobs = new ArrayList<ScheduledJob>();
        jobs.add(job);
        when(scheduledJobService.getScheduledJobsByType(job.vmId, ScheduledJobType.ZOMBIE)).thenReturn(jobs);

        vm = new VirtualMachine();
        vm.hfsVmId = 1324;
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.serverType = ServerType.Type.VIRTUAL;
        when(virtualMachineService.getVirtualMachine(job.vmId)).thenReturn(vm);

        VmAction vma = new VmAction();
        vma.vmActionId = 3323;
        vma.vmId = vm.hfsVmId;
        vma.state = VmAction.Status.COMPLETE;
        when(vmService.endRescueVm(vm.hfsVmId)).thenReturn(vma);
        when(vmService.startVm(vm.hfsVmId)).thenReturn(vma);
        when(vmService.getVmAction(vma.vmId, vma.vmActionId)).thenReturn(vma);

        vmCdnSite = new VmCdnSite();
        vmCdnSite.vmId = job.vmId;
        vmCdnSite.siteId = "fakeSiteId";
        when(cdnDataService.getActiveCdnSitesOfVm(job.vmId)).thenReturn(Collections.singletonList(vmCdnSite));

        cdnDetail = new CdnDetail();
        cdnDetail.status = CdnStatus.SUCCESS;
        when(cdnService.getCdnSiteDetail(any(), anyString(), any(), anyBoolean())).thenReturn(cdnDetail);

        customerId = UUID.randomUUID();
        vmCredit = mock(VirtualMachineCredit.class);
        when(vmCredit.getCustomerId()).thenReturn(customerId);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(vmCredit);

        PanoptaDetail panoptaDetail = new PanoptaDetail(job.vmId, "partnerCustomerKey",
                                                        "customerKey", 3, "serverKey",
                                                        Instant.now(), Instant.MAX, null);
        when(panoptaDataService.getPanoptaDetails(job.vmId)).thenReturn(panoptaDetail);

        injector = Guice.createInjector(binder -> {
            binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
            binder.bind(VmService.class).toInstance(vmService);
            binder.bind(ScheduledJobService.class).toInstance(scheduledJobService);
            binder.bind(SchedulerWebService.class).toInstance(schedulerWebService);
            binder.bind(ActionService.class).toInstance(actionService);
            binder.bind(PanoptaDataService.class).toInstance(panoptaDataService);
            binder.bind(PanoptaService.class).toInstance(panoptaService);
            binder.bind(CdnDataService.class).toInstance(cdnDataService);
            binder.bind(CdnService.class).toInstance(cdnService);
        });

        command = new Vps4ReviveZombieVm(actionService, virtualMachineService, creditService, cdnDataService);
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    @Test
    public void testReviveZombieVm() {
        Vps4ReviveZombieVm.Request request = new Vps4ReviveZombieVm.Request();
        request.vmId = job.vmId;
        request.newCreditId = UUID.randomUUID();
        request.oldCreditId = UUID.randomUUID();
        vm.name = "testVm";

        Map<ProductMetaField, String> productMeta = new HashMap<>();
        when(creditService.getProductMeta(request.oldCreditId)).thenReturn(productMeta);

        command.execute(context, request);

        String product = Utils.getProductForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        String group = Utils.getJobGroupForJobRequestClass(Vps4ZombieCleanupJobRequest.class);

        verify(virtualMachineService, times(1)).reviveZombieVm(request.vmId, request.newCreditId);
        verify(schedulerWebService, times(1)).deleteJob(product, group, job.id);
        verify(creditService, times(1)).setCommonName(request.newCreditId, "testVm");
        verify(creditService, times(1)).updateProductMeta(request.newCreditId, productMeta);
        verify(vmService, times(0)).endRescueVm(vm.hfsVmId);
        verify(vmService, times(1)).startVm(vm.hfsVmId);
        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(request.vmId);
        verify(cdnService, times(1)).updateCdnSite(customerId, "fakeSiteId", CdnCacheLevel.CACHING_OPTIMIZED, CdnBypassWAF.DISABLED);
    }

    @Test
    public void testReviveZombieDedicated() {
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
        Vps4ReviveZombieVm.Request request = new Vps4ReviveZombieVm.Request();
        request.vmId = job.vmId;
        request.newCreditId = UUID.randomUUID();
        request.oldCreditId = UUID.randomUUID();
        vm.name = "testDed";

        Map<ProductMetaField, String> productMeta = new HashMap<>();
        when(creditService.getProductMeta(request.oldCreditId)).thenReturn(productMeta);

        command.execute(context, request);

        String product = Utils.getProductForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        String group = Utils.getJobGroupForJobRequestClass(Vps4ZombieCleanupJobRequest.class);

        verify(virtualMachineService, times(1)).reviveZombieVm(request.vmId, request.newCreditId);
        verify(schedulerWebService, times(1)).deleteJob(product, group, job.id);
        verify(creditService, times(1)).setCommonName(request.newCreditId, "testDed");
        verify(creditService, times(1)).updateProductMeta(request.newCreditId, productMeta);
        verify(vmService, times(1)).endRescueVm(vm.hfsVmId);
        verify(vmService, times(0)).startVm(vm.hfsVmId);
        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(request.vmId);
        verify(cdnService, times(1)).updateCdnSite(customerId, "fakeSiteId", CdnCacheLevel.CACHING_OPTIMIZED, CdnBypassWAF.DISABLED);
    }
}
