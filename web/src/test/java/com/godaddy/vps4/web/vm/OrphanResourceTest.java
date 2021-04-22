package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.DataCenterService;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.Orphans;
import com.godaddy.vps4.web.credit.CreditResource;

import gdg.hfs.vhfs.network.NetworkServiceV2;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class OrphanResourceTest {
    private OrphanResource resource;
    private VmResource vmResource;
    private CreditResource creditResource;
    private VmSnapshotResource snapshotResource;
    private ProjectService projectService;

    private NodePingService hfsNodepingService;
    private NetworkServiceV2 networkService;

    private MonitoringMeta monitoringMeta;

    private UUID vmId = UUID.randomUUID();
    private VirtualMachine vm;
    private Project project;

    private String SGID = "vps4-unit-test";

    @Before
    public void setupTest(){
        vm = new VirtualMachine();
        vm.vmId = vmId;
        vm.orionGuid = UUID.randomUUID();
        vm.projectId = 1111;
        vm.image = new Image();
        vm.image.controlPanel = Image.ControlPanel.CPANEL;
        vm.hfsVmId = 1234;

        ServerSpec spec = new ServerSpec();
        spec.serverType = new ServerType();
        spec.serverType.serverType = ServerType.Type.VIRTUAL;
        vm.spec = spec;

        projectService = mock(ProjectService.class);
        project = new Project(1111, "test project", SGID, null, null);
        when(projectService.getProject(1111)).thenReturn(project);

        vmResource = mock(VmResource.class);
        when(vmResource.getVm(vmId)).thenReturn(vm);
        creditResource = mock(CreditResource.class);
        snapshotResource = mock(VmSnapshotResource.class);


        hfsNodepingService = mock(NodePingService.class);
        networkService = mock(NetworkServiceV2.class);

        monitoringMeta = mock(MonitoringMeta.class);

        when(monitoringMeta.getAccountId()).thenReturn(Long.valueOf(3333));

        resource = new OrphanResource(vmResource, creditResource, snapshotResource,
                projectService, hfsNodepingService, networkService, monitoringMeta);


    }

    @Test(expected = Vps4Exception.class)
    public void testInvalidCreditStillAssignedToCredit() {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("product_id", vm.vmId.toString());
        VirtualMachineCredit virtualMachineCredit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
            .withProductMeta(productMeta)
            .build();

        when(creditResource.getCredit(vm.orionGuid)).thenReturn(virtualMachineCredit);
        resource.getOrphanedResources(vm.vmId);
    }

    @Test
    public void testGetOrphanedVmNoHfsVm(){
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(1234, items.hfsVmId);
        assertNull(items.hfsVmStatus);
    }

    @Test
    public void testGetOrphanedVmHfsErrors(){
        when(vmResource.getVmFromVmVertical(1234)).thenThrow(new Vps4Exception("TEST EXCEPTION", "this is just a test"));
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(1234, items.hfsVmId);
        assertNull(items.hfsVmStatus);
    }

    @Test
    public void testGetOrphanedVmHfsReturnsVm(){
        Vm hfsVm = new Vm();
        hfsVm.status = "DESTROYED";
        when(vmResource.getVmFromVmVertical(1234)).thenReturn(hfsVm);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(1234, items.hfsVmId);
        assertEquals("DESTROYED", items.hfsVmStatus);
    }

    @Test
    public void testGetOrphanedSgIdNoProject(){
        when(projectService.getProject(1111)).thenReturn(null);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(null, items.sgid);
    }

    @Test
    public void testGetOrphanedSgId(){
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(project.getVhfsSgid(), items.sgid);
    }

    @Test
    public void testGetOrphanedIpIpIsNull(){
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(null, items.ip);
    }

    private IpAddress getPrimaryIp(){
        IpAddress ip = new IpAddress();
        ip.hfsAddressId = 4545;
        ip.validUntil= Instant.MAX;
        ip.ipAddress = "123.32.2.1";
        return ip;
    }

    @Test
    public void testGetOrphanedIpIpIsDedicated(){
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
        IpAddress ip = getPrimaryIp();
        vm.primaryIpAddress = ip;

        gdg.hfs.vhfs.network.IpAddress hfsIp = new gdg.hfs.vhfs.network.IpAddress();
        hfsIp.sgid = SGID;
        when(networkService.getAddress(4545)).thenReturn(hfsIp);

        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertNull(items.ip);
    }

    @Test
    public void testGetOrphanedIpIpIsMarkedDestroyed(){
        IpAddress ip = getPrimaryIp();
        ip.validUntil= Instant.now().minusSeconds(600);  // mark ip as destroyed
        vm.primaryIpAddress = ip;

        gdg.hfs.vhfs.network.IpAddress hfsIp = new gdg.hfs.vhfs.network.IpAddress();
        hfsIp.sgid = SGID;
        when(networkService.getAddress(4545)).thenReturn(hfsIp);

        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(null, items.ip);
    }

    @Test
    public void testGetOrphanedIpHfsIpIsNull(){
        IpAddress ip = getPrimaryIp();
        vm.primaryIpAddress = ip;

        gdg.hfs.vhfs.network.IpAddress hfsIp = new gdg.hfs.vhfs.network.IpAddress();
        hfsIp.sgid = SGID;
        when(networkService.getAddress(4545)).thenReturn(null);  //hfs IP is null
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(null, items.ip);
    }

    @Test
    public void testGetOrphanedIpSgidDoesntMatchVm(){
        IpAddress ip = getPrimaryIp();
        vm.primaryIpAddress = ip;

        gdg.hfs.vhfs.network.IpAddress hfsIp = new gdg.hfs.vhfs.network.IpAddress();
        hfsIp.sgid = "non-matching-sgid";             //hfs SGID doesn't match vm's sgid
        when(networkService.getAddress(4545)).thenReturn(hfsIp);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(null, items.ip);
    }

    @Test
    public void testGetOrphanedIpHfsThrowsError(){
        IpAddress ip = getPrimaryIp();
        vm.primaryIpAddress = ip;

        when(networkService.getAddress(4545)).thenThrow(new Vps4Exception("Test Exception", "this is a test exception"));
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(null, items.ip);
    }

    @Test
    public void testGetOrphanedIp(){
        IpAddress ip = getPrimaryIp();
        vm.primaryIpAddress = ip;

        gdg.hfs.vhfs.network.IpAddress hfsIp = new gdg.hfs.vhfs.network.IpAddress();
        hfsIp.sgid = SGID;
        when(networkService.getAddress(4545)).thenReturn(hfsIp);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(hfsIp, items.ip);
    }

    @Test
    public void getOrphanedNodepingAccountNoAccount(){
        IpAddress ip = getPrimaryIp();
        vm.primaryIpAddress = ip;
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertNull(items.nodePingCheck);
    }

    @Test
    public void getOrphanedNodepingAccountHfsThrowsException(){
        IpAddress ip = getPrimaryIp();
        ip.pingCheckId = Long.valueOf(9999);
        vm.primaryIpAddress = ip;

        when(hfsNodepingService.getCheck(3333, 9999)).thenThrow(new Vps4Exception("Test Exception", "this is a test exception"));
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertNull(items.nodePingCheck);
    }

    @Test
    public void getOrphanedNodepingAccountHfsReturnsNull(){
        IpAddress ip = getPrimaryIp();
        ip.pingCheckId = Long.valueOf(9999);
        vm.primaryIpAddress = ip;

        when(hfsNodepingService.getCheck(3333, 9999)).thenReturn(null);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertNull(items.nodePingCheck);
    }

    @Test
    public void getOrphanedNodepingAccountAccountNotActive(){
        IpAddress ip = getPrimaryIp();
        ip.pingCheckId = Long.valueOf(9999);
        vm.primaryIpAddress = ip;

        NodePingCheck npc = new NodePingCheck();
        npc.enabled = "not active";
        npc.target = "123.32.2.1";

        when(hfsNodepingService.getCheck(3333, 9999)).thenReturn(npc);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertNull(items.nodePingCheck);
    }

    @Test
    public void getOrphanedNodepingAccountAccountTargetIsntPrimaryIp(){
        IpAddress ip = getPrimaryIp();
        ip.pingCheckId = Long.valueOf(9999);
        vm.primaryIpAddress = ip;

        NodePingCheck npc = new NodePingCheck();
        npc.enabled = "active";
        npc.target = "1.1.1.1";

        when(hfsNodepingService.getCheck(3333, 9999)).thenReturn(npc);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertNull(items.nodePingCheck);
    }

    @Test
    public void getOrphanedNodepingAccountAccount(){
        IpAddress ip = getPrimaryIp();
        ip.pingCheckId = Long.valueOf(9999);
        vm.primaryIpAddress = ip;

        NodePingCheck npc = new NodePingCheck();
        npc.enabled = "active";
        npc.target = ip.ipAddress;

        when(hfsNodepingService.getCheck(3333, 9999)).thenReturn(npc);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(npc, items.nodePingCheck);
    }

    @Test
    public void getOrphanedSnapshots(){
        List<Snapshot>  snapshotList = new ArrayList<Snapshot>();
        when(snapshotResource.getSnapshotsForVM(vm.vmId)).thenReturn(snapshotList);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(snapshotList, items.snapshotList);
    }

}
