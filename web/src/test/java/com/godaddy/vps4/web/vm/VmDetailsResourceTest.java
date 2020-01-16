package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;

public class VmDetailsResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private CreditService creditService = mock(CreditService.class);
    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);
    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);

    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private Long hfsVmId = 42L;
    private VirtualMachine vps4Vm = new VirtualMachine();

    private VmDetailsResource vmDetailsResource = new VmDetailsResource(vmResource, creditService,
            schedulerWebService, panoptaDataService);


    @Before
    public void setUp() {
        vps4Vm.hfsVmId = hfsVmId;
        vps4Vm.orionGuid = orionGuid;
        when(vmResource.getVm(vmId)).thenReturn(vps4Vm);

        Vm hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        hfsVm.status = "ACTIVE";
        hfsVm.running = true;
        hfsVm.useable = true;
        hfsVm.resourceId = "ns3210123.ip-123-45-67.eu";
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);

        credit = mock(VirtualMachineCredit.class);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
    }

    @Test
    public void testGetDetailsForHfsVm() {
        VirtualMachineDetails details = vmDetailsResource.getVirtualMachineDetails(vmId);
        assertEquals(hfsVmId, details.vmId);
        assertEquals("ACTIVE", details.status);
        assertTrue(details.running);
        assertTrue(details.useable);
        assertNotNull(details.resourceId);

        // Verify class has toString method
        assertNotNull(details.toString());
    }

    @Test
    public void testGetDetailsForNullVm() {
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(null);
        VirtualMachineDetails details = vmDetailsResource.getVirtualMachineDetails(vmId);
        assertNull(details.vmId);
        assertEquals("REQUESTING", details.status);
        assertFalse(details.running);
        assertFalse(details.useable);
        assertNull(details.resourceId);
    }

    @Test
    public void testGetMoreDetailsForHfsVm() {
        Vm hfsVm = vmDetailsResource.getMoreDetails(vmId);
        assertNotNull(hfsVm);
        assertEquals((long)hfsVmId, hfsVm.vmId);
        assertEquals("ACTIVE", hfsVm.status);
        assertTrue(hfsVm.running);
        assertTrue(hfsVm.useable);
        assertNotNull(hfsVm.resourceId);
    }

    @Test
    public void testGetVirtualMachineWithDetailsContainsDataCenter() {
        DataCenter dc = new DataCenter(5, "testDc");
        when(credit.getDataCenter()).thenReturn(dc);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(dc, withDetails.dataCenter);
    }

    @Test
    public void testGetWithDetailsContainsShopperId() {
        String shopperId = "testShopperId";
        when(credit.getShopperId()).thenReturn(shopperId);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(shopperId, withDetails.shopperId);
    }

    @Test
    public void testGetWithDetailsWithNullBackupId() {
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertNull(withDetails.backupJobId);
        assertNull(withDetails.autoSnapshots.nextAt);
    }

    @Test
    public void testGetWithDetailsWhenBackupLookupFails() {
        vps4Vm.backupJobId = UUID.randomUUID();
        when(schedulerWebService.getJob("vps4", "backups", vps4Vm.backupJobId)).thenReturn(null);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertNotNull(withDetails.backupJobId);
        assertNull(withDetails.autoSnapshots.nextAt);
    }

    @Test
    public void testGetWithDetailsContainsScheduledBackup() {
        vps4Vm.backupJobId = UUID.randomUUID();
        Instant nextRun = Instant.now();
        JobRequest jobReq = mock(JobRequest.class);
        jobReq.repeatIntervalInDays = 7;
        SchedulerJobDetail job = new SchedulerJobDetail(UUID.randomUUID(), nextRun, jobReq, false);
        when(schedulerWebService.getJob("vps4", "backups", vps4Vm.backupJobId)).thenReturn(job);

        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(nextRun, withDetails.autoSnapshots.nextAt);
        assertEquals(7, withDetails.autoSnapshots.repeatIntervalInDays);
    }

    @Test
    public void testGetWithDetailsContainsPanoptaDetails() {
        PanoptaServerDetails details = new PanoptaServerDetails();
        details.setServerId(23L);
        details.setServerKey("panopta-server-key");
        when(panoptaDataService.getPanoptaServerDetails(vmId)).thenReturn(details);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(23L, withDetails.monitoringAgent.getServerId());
        assertEquals("panopta-server-key", withDetails.monitoringAgent.getServerKey());
    }

    @Test
    public void testGetWithDetailsWithoutPanopta() {
        when(panoptaDataService.getPanoptaServerDetails(vmId)).thenReturn(null);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertNull(withDetails.monitoringAgent);
    }

}
