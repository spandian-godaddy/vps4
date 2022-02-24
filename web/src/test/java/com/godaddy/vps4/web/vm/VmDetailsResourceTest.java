package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;

public class VmDetailsResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private CreditService creditService = mock(CreditService.class);
    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);
    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private VmZombieResource vmZombieResource = mock(VmZombieResource.class);
    private NetworkService networkService = mock(NetworkService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);

    private GDUser user = mock(GDUser.class);

    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private Long hfsVmId = 42L;
    private VirtualMachine vps4Vm = new VirtualMachine();

    private VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();

    private VmDetailsResource vmDetailsResource = new VmDetailsResource(vmResource, creditService,
            schedulerWebService, panoptaDataService, vmZombieResource, user, networkService, virtualMachineService);


    @Before
    public void setUp() {
        vps4Vm.hfsVmId = hfsVmId;
        vps4Vm.orionGuid = orionGuid;
        vps4Vm.canceled = Instant.MAX;
        when(vmResource.getVm(vmId)).thenReturn(vps4Vm);

        Vm hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        hfsVm.status = "ACTIVE";
        hfsVm.running = true;
        hfsVm.useable = true;
        hfsVm.resourceId = "ns3210123.ip-123-45-67.eu";
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);

        vmExtendedInfoMock.provider = "nocfox";
        vmExtendedInfoMock.resource = "openstack";
        Extended extendedMock = new Extended();
        extendedMock.hypervisorHostname = "n3plztncldhv001-02.prod.ams3.gdg";
        vmExtendedInfoMock.extended = extendedMock;
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(vmExtendedInfoMock);

        credit = mock(VirtualMachineCredit.class);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);

        when(virtualMachineService.getImportedVm(vmId)).thenReturn(vmId);
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

    @Test
    public void testGetWithDetailsContainsZombieJobDetail() {
        // zombie the vm
        vps4Vm.canceled = Instant.now().minus(10, ChronoUnit.MINUTES);
        UUID jobId = UUID.randomUUID();
        Instant nextRun = Instant.now().plus(7, ChronoUnit.DAYS);
        SchedulerJobDetail job = mock(SchedulerJobDetail.class);
        List<SchedulerJobDetail> scheduledZombieCleanupJobs = Collections.singletonList(job);
        when(vmZombieResource.getScheduledZombieVmDelete(eq(vmId))).thenReturn(scheduledZombieCleanupJobs);

        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);

        assertNotNull(withDetails.scheduledZombieCleanupJobs);
        assertFalse(withDetails.scheduledZombieCleanupJobs.isEmpty());
        assertEquals(1, withDetails.scheduledZombieCleanupJobs.size());
    }

    @Test
    public void withDetailsDoesNotReturnZombieCleanupJobIfVmIsNotZombie() {
        List<SchedulerJobDetail> scheduledZombieCleanupJobs = Collections.emptyList();
        when(vmZombieResource.getScheduledZombieVmDelete(eq(vmId))).thenReturn(scheduledZombieCleanupJobs);

        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertTrue(withDetails.scheduledZombieCleanupJobs.isEmpty());
    }

    @Test
    public void testGetWithDetailsContainsNullHvHostnameForDed4() {
        when(credit.isDed4()).thenReturn(Boolean.TRUE);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(null, withDetails.hypervisorHostname);
    }

    @Test
    public void testGetWithDetailsContainsNullHvHostnameForNonEmployee() {
        when(user.isEmployee()).thenReturn(Boolean.FALSE);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(null, withDetails.hypervisorHostname);
    }

    @Test
    public void testGetWithDetailsContainsHvHostname() {
        when(user.isEmployee()).thenReturn(Boolean.TRUE);
        when(credit.isDed4()).thenReturn(Boolean.FALSE);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(vmExtendedInfoMock.extended.hypervisorHostname, withDetails.hypervisorHostname);
    }

    @Test
    public void testGetWithDetailsContainsByNullHvHostnameDueToHfsException() {
        when(user.isEmployee()).thenReturn(Boolean.TRUE);
        when(credit.isDed4()).thenReturn(Boolean.FALSE);
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(null);
        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);
        assertEquals(null, withDetails.hypervisorHostname);
    }


    @Test
    public void testGetWithDetailsContainsAdditionalIps() {
        List<IpAddress> additionalIps = new ArrayList<>();

        IpAddress secondIp = new IpAddress();
        secondIp.validUntil = Instant.MAX;
        secondIp.ipAddressType = IpAddress.IpAddressType.SECONDARY;
        additionalIps.add(secondIp);

        when(networkService.getVmSecondaryAddress(hfsVmId)).thenReturn(additionalIps);

        VirtualMachineWithDetails withDetails = vmDetailsResource.getVirtualMachineWithDetails(vmId);

        assertNotNull(withDetails.additionalIps);
        assertFalse(withDetails.additionalIps.isEmpty());
        assertEquals(1, withDetails.additionalIps.size());
    }

    @Test
    public void testGetVmExtendedDetailsNullFromHfs() {
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(null);

        VmExtendedInfo vmExtendedInfo = vmDetailsResource.getVmExtendedDetails(vmId);

        assertNull(vmExtendedInfo);
    }

    @Test
    public void testGetVmExtendedDetailsNullTaskPowerStates() {
        VmExtendedInfo value = new VmExtendedInfo();
        value.extended = new Extended();
        value.extended.hypervisorHostname = "hostname";
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(value);

        VmExtendedInfo vmExtendedInfo = vmDetailsResource.getVmExtendedDetails(vmId);

        assertNull(vmExtendedInfo.extended.powerState);
        assertNull(vmExtendedInfo.extended.taskState);
    }

    @Test
    public void testGetVmExtendedDetails() {
        VmExtendedInfo value = new VmExtendedInfo();
        value.extended = new Extended();
        value.extended.hypervisorHostname = "hostname";
        value.extended.powerState = "paused";
        value.extended.taskState = "image_snapshot";
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(value);

        VmExtendedInfo vmExtendedInfo = vmDetailsResource.getVmExtendedDetails(vmId);

        assertEquals(value.extended.taskState, vmExtendedInfo.extended.taskState);
        assertEquals(value.extended.powerState, vmExtendedInfo.extended.powerState);
    }
}
