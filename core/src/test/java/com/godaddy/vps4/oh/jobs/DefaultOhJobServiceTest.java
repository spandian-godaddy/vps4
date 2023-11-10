package com.godaddy.vps4.oh.jobs;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.OhResponse;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultOhJobServiceTest {
    @Mock private OhApiJobService ohApiJobService1;
    @Mock private OhApiJobService ohApiJobService2;
    @Mock private Map<String, OhApiJobService> ohApiJobServices;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private VmService vmService;

    @Mock private OhResponse<OhJob> job;
    @Mock private VirtualMachine vm;
    @Mock private Vm hfsVm;

    private DefaultOhJobService service;

    @Before
    public void setUp() {
        setUpMockVm();
        setUpMockHfsVm();
        setUpMockJob();
        when(ohApiJobServices.get("zone1")).thenReturn(ohApiJobService1);
        when(ohApiJobServices.get("zone2")).thenReturn(ohApiJobService2);
        when(virtualMachineService.getHfsVmIdByVmId(vm.vmId)).thenReturn(vm.hfsVmId);
        when(vmService.getVm(hfsVm.vmId)).thenReturn(hfsVm);
        when(ohApiJobService1.getJob(any(UUID.class))).thenReturn(job);
        when(ohApiJobService2.getJob(any(UUID.class))).thenReturn(job);
        service = new DefaultOhJobService(ohApiJobServices, virtualMachineService, vmService);
    }

    private void setUpMockJob() {
        OhJob jobValue = mock(OhJob.class);
        jobValue.id = UUID.randomUUID();
        when(job.value()).thenReturn(jobValue);
    }

    private void setUpMockVm() {
        vm.vmId = UUID.fromString("d5500543-8851-48e9-b6d1-4352d969db76");
        vm.hfsVmId = 42522L;
    }

    private void setUpMockHfsVm() {
        hfsVm.vmId = vm.hfsVmId;
        hfsVm.resourceRegion = "zone1";
        hfsVm.resourceId = "22e97fb5-e7f7-41fd-a199-7116350de05d";
    }

    @Test
    public void getJobGetsCorrectParameters() {
        service.getJob(vm.vmId, job.value().id);
        verify(virtualMachineService).getHfsVmIdByVmId(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiJobServices).get(hfsVm.resourceRegion);
        verify(ohApiJobService1).getJob(job.value().id);
        verify(ohApiJobService2, never()).getJob(any(UUID.class));
    }

    @Test
    public void getJobCallsCorrectZone() {
        hfsVm.resourceRegion = "zone2";
        service.getJob(vm.vmId, job.value().id);
        verify(ohApiJobService1, never()).getJob(any(UUID.class));
        verify(ohApiJobService2).getJob(job.value().id);
    }

    @Test
    public void getJobReturnsJob() {
        OhJob result = service.getJob(vm.vmId, job.value().id);
        assertSame(job.value(), result);
    }
}
