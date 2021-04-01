package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.web.Vps4Exception;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.snapshot.SnapshotResource;

public class VmSnapshotResourceTest {

    private GDUser user;
    private CreditService creditService;
    private SnapshotService snapshotService;
    private SnapshotResource snapshotResource;
    private VirtualMachineService virtualMachineService;
    private VmService vmService;
    private Config config;
    private VirtualMachine testVm;
    private VmSnapshotResource resource;

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        creditService = mock(CreditService.class);
        snapshotResource = mock(SnapshotResource.class);
        snapshotService = mock(SnapshotService.class);
        virtualMachineService = mock(VirtualMachineService.class);
        vmService = mock(VmService.class);
        config = mock(Config.class);
        resource = new VmSnapshotResource(user, creditService, snapshotResource, snapshotService, virtualMachineService,
                                          vmService, config);

        UUID vmId = UUID.randomUUID();
        testVm = new VirtualMachine();
        testVm.vmId = vmId;
        testVm.validOn = Instant.now();
        testVm.canceled = Instant.MAX;
        testVm.validUntil = Instant.MAX;
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(testVm);

        when(config.get("vps4.autobackup.concurrentLimit","50")).thenReturn("70");
        when(snapshotService.totalSnapshotsInProgress()).thenReturn(69);
    }

    @Test
    public void cannotSnapshotNowIfDcLimitReached() {
        when(snapshotService.totalSnapshotsInProgress()).thenReturn(71);
        Assert.assertFalse(resource.canSnapshotNow(testVm.vmId));
    }

    @Test
    public void canSnapshotNowIfDcLimitNotReachedAndHvLimitCheckDisabled() {
        when(config.get("vps4.autobackup.checkHvConcurrentLimit")).thenReturn("false");
        Assert.assertTrue(resource.canSnapshotNow(testVm.vmId));
    }

    @Test
    public void canSnapshotNowIfDcLimitNotReachedAndNoHvInfoRetrieved() {
        when(config.get("vps4.autobackup.checkHvConcurrentLimit")).thenReturn("true");
        when(vmService.getVmExtendedInfo(anyLong())).thenReturn(null);
        Assert.assertTrue(resource.canSnapshotNow(testVm.vmId));
    }

    @Test
    public void canSnapshotNowIfDcLimitNotReachedAndHvLimitNotReached() {
        when(config.get("vps4.autobackup.checkHvConcurrentLimit")).thenReturn("true");
        VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();
        vmExtendedInfoMock.provider = "nocfox";
        vmExtendedInfoMock.resource = "openstack";
        Extended extendedMock = new Extended();
        extendedMock.hypervisorHostname = "n3plztncldhv001-02.prod.ams3.gdg";
        vmExtendedInfoMock.extended = extendedMock;
        when(vmService.getVmExtendedInfo(anyLong())).thenReturn(vmExtendedInfoMock);
        when(snapshotService.getVmIdWithInProgressSnapshotOnHv(anyString())).thenReturn(null);
        Assert.assertTrue(resource.canSnapshotNow(testVm.vmId));
    }

    @Test
    public void cannotSnapshotNowIfDcLimitNotReachedAndHvLimitReached() {
        when(config.get("vps4.autobackup.checkHvConcurrentLimit")).thenReturn("true");
        VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();
        vmExtendedInfoMock.provider = "nocfox";
        vmExtendedInfoMock.resource = "openstack";
        Extended extendedMock = new Extended();
        extendedMock.hypervisorHostname = "n3plztncldhv001-02.prod.ams3.gdg";
        vmExtendedInfoMock.extended = extendedMock;
        when(vmService.getVmExtendedInfo(anyLong())).thenReturn(vmExtendedInfoMock);
        when(snapshotService.getVmIdWithInProgressSnapshotOnHv(anyString())).thenReturn(UUID.randomUUID());
        Assert.assertFalse(resource.canSnapshotNow(testVm.vmId));
    }

    @Test
    public void runsSnapshotResource() {
        when(config.get("vps4.snapshot.currentlyPaused")).thenReturn("false");
        VmSnapshotResource.VmSnapshotRequest vmSnapshotRequest = new VmSnapshotResource.VmSnapshotRequest();
        vmSnapshotRequest.snapshotType= SnapshotType.ON_DEMAND;
        vmSnapshotRequest.name = "testSnapshot";
        resource.createSnapshot(testVm.vmId,vmSnapshotRequest);
        SnapshotResource.SnapshotRequest snapshotRequest = new SnapshotResource.SnapshotRequest();
        verify(snapshotResource, times(1))
                .createSnapshot(any());
    }

    @Test
    public void canSnapshotNowIfPauseSnapshotFlagIsFalse() {
        when(config.get("vps4.snapshot.currentlyPaused")).thenReturn("false");
        VmSnapshotResource.VmSnapshotRequest snapshotRequest = new VmSnapshotResource.VmSnapshotRequest();
        snapshotRequest.snapshotType= SnapshotType.ON_DEMAND;
        snapshotRequest.name = "testSnapshot";
        Assert.assertTrue(resource.canSnapshotNow(testVm.vmId));
    }

    @Test(expected = Vps4Exception.class)
    public void throwsExceptionIfPauseSnapshotFlagIsTrue() {
        when(config.get("vps4.snapshot.currentlyPaused")).thenReturn("true");
        VmSnapshotResource.VmSnapshotRequest snapshotRequest = new VmSnapshotResource.VmSnapshotRequest();
        snapshotRequest.snapshotType= SnapshotType.ON_DEMAND;
        snapshotRequest.name = "testSnapshot";
        resource.createSnapshot(testVm.vmId,snapshotRequest);

    }
    @Test
    public void cannotSnapshotNowIfPauseSnapshotFlagIsTrue() {
        when(config.get("vps4.snapshot.currentlyPaused")).thenReturn("true");
        VmSnapshotResource.VmSnapshotRequest snapshotRequest = new VmSnapshotResource.VmSnapshotRequest();
        snapshotRequest.snapshotType= SnapshotType.ON_DEMAND;
        snapshotRequest.name = "testSnapshot";
        Assert.assertFalse(resource.canSnapshotNow(testVm.vmId));
    }
}
