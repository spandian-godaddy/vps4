package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.Orphans;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.credit.Vps4Credit;

import gdg.hfs.vhfs.network.NetworkServiceV2;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrphanResourceTest {
    private OrphanResource resource;
    private VmResource vmResource;
    private CreditResource creditResource;
    private VmSnapshotResource snapshotResource;
    private ProjectService projectService;

    private NetworkServiceV2 networkService;

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
        project = new Project(1111, "test project", SGID, null, null, 321);
        when(projectService.getProject(1111)).thenReturn(project);

        vmResource = mock(VmResource.class);
        when(vmResource.getVm(vmId)).thenReturn(vm);
        creditResource = mock(CreditResource.class);
        snapshotResource = mock(VmSnapshotResource.class);

        networkService = mock(NetworkServiceV2.class);

        resource = new OrphanResource(vmResource, creditResource, snapshotResource, projectService, networkService);


    }

    @Test(expected = Vps4Exception.class)
    public void testInvalidCreditStillAssignedToCredit() {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("product_id", vm.vmId.toString());
        DataCenterService dataCenterService = mock(DataCenterService.class);
        when(dataCenterService.getDataCenter(1)).thenReturn(new DataCenter(1, "test"));
        VirtualMachineCredit virtualMachineCredit = new VirtualMachineCredit.Builder()
            .withProductMeta(productMeta)
            .build();
        Vps4Credit vps4Credit = new Vps4Credit(virtualMachineCredit, dataCenterService);
        when(creditResource.getCredit(vm.orionGuid)).thenReturn(vps4Credit);
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
    public void getOrphanedSnapshots(){
        List<Snapshot>  snapshotList = new ArrayList<Snapshot>();
        when(snapshotResource.getSnapshotsForVM(vm.vmId)).thenReturn(snapshotList);
        Orphans items = resource.getOrphanedResources(vm.vmId);
        assertEquals(snapshotList, items.snapshotList);
    }

}
