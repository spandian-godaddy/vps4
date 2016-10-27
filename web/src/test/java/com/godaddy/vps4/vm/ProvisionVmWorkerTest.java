package com.godaddy.vps4.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.hfs.ProvisionVMRequest;
import com.godaddy.vps4.hfs.Vm;
import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.vm.CreateVmStep;
import com.godaddy.vps4.web.vm.ProvisionVmWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.AddressAction.Status;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;

public class ProvisionVmWorkerTest {

    private VmService vmService;
    private NetworkService hfsNetworkSerivce;
    private ExecutorService threadPool;
    private com.godaddy.vps4.network.NetworkService vps4NetworkService;

    private IpAddress ip;
    private VmAction vmActionInProgress;
    private VmAction vmActionAfter;
    private CreateVmAction action;
    private Vm vm;
    private AddressAction addressAction;
    private VirtualMachineService virtualMachineService;
    private UUID orionGuid;
    private String name;
    private long projectId;
    private int specId;
    private int managedLevel; 
    private int imageId;

    @Before
    public void setup() {

        vmService = Mockito.mock(VmService.class);
        hfsNetworkSerivce = Mockito.mock(NetworkService.class);
        threadPool = Executors.newCachedThreadPool();
        vps4NetworkService = Mockito.mock(com.godaddy.vps4.network.NetworkService.class);
        virtualMachineService = Mockito.mock(VirtualMachineService.class);
        
        ip = new IpAddress();
        ip.address = "127.0.0.1";
        ip.addressId = 123;
        ip.status = IpAddress.Status.UNBOUND;

        vmActionInProgress = new VmAction();
        vmActionInProgress.state = "IN_PROGRESS";
        vmActionInProgress.vmId = 12;
        vmActionInProgress.vmActionId = 1;
        vmActionInProgress.tickNum = 3;

        vmActionAfter = new VmAction();
        vmActionAfter.state = "COMPLETE";
        vmActionAfter.vmId = vmActionInProgress.vmId;
        vmActionAfter.vmActionId = vmActionInProgress.vmActionId;

        action = new CreateVmAction();
        action.status = ActionStatus.IN_PROGRESS;
        Project project = new Project(1, "testProject", "vps4-1", 1, Instant.now(), Instant.MAX);
        action.project = project;

        vm = new Vm();
        vm.vmId = vmActionAfter.vmId;

        addressAction = new AddressAction();
        addressAction.status = Status.COMPLETE;
        addressAction.addressId = ip.addressId;
        
        
        orionGuid = UUID.randomUUID();
        name = "testName";
        projectId = 1;
        specId = 1;
        managedLevel = 1; 
        imageId = 1;
    }
    
    private void runProvisionVmWorker(){
        ProvisionVmWorker worker = new ProvisionVmWorker(vmService, hfsNetworkSerivce, action, threadPool, vps4NetworkService,
                virtualMachineService, orionGuid, name, projectId, specId, managedLevel, imageId);
        worker.run();
    }

    @Test
    public void provisionVmTest() throws InterruptedException {

        Mockito.when(vmService.createVm(action.hfsProvisionRequest)).thenReturn(vmActionInProgress);
        Mockito.when(vmService.getVmAction(vmActionInProgress.vmId, vmActionInProgress.vmActionId)).thenReturn(vmActionAfter);
        Mockito.when(vmService.getVm(vmActionAfter.vmId)).thenReturn(vm);

        Mockito.when(hfsNetworkSerivce.acquireIp(action.project.getVhfsSgid())).thenReturn(addressAction);
        Mockito.when(hfsNetworkSerivce.getAddress(addressAction.addressId)).thenReturn(ip);
        Mockito.when(hfsNetworkSerivce.bindIp(ip.addressId, vmActionInProgress.vmId)).thenReturn(addressAction);

        runProvisionVmWorker();

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        assertEquals(vmActionAfter.vmId, action.vm.vmId);
        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals(ip.addressId, action.ip.addressId);
        assertEquals(CreateVmStep.SetupComplete, action.step);

        verify(vmService, times(1)).createVm(any(ProvisionVMRequest.class));
        verify(hfsNetworkSerivce, times(1)).acquireIp(action.project.getVhfsSgid());
        verify(hfsNetworkSerivce, times(1)).bindIp(ip.addressId, vmActionAfter.vmId);
    }

    @Test
    public void provisionVmAllocateIpFailsTest() throws InterruptedException {

        addressAction.status = Status.FAILED;

        Mockito.when(hfsNetworkSerivce.acquireIp(action.project.getVhfsSgid())).thenReturn(addressAction);

        runProvisionVmWorker();

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        assertEquals(ActionStatus.ERROR, action.status);
        verify(hfsNetworkSerivce, times(1)).acquireIp(action.project.getVhfsSgid());
        assertNull(action.vm);
        assertNull(action.ip);
        assertEquals(CreateVmStep.RequestingIPAddress, action.step);

        verify(vmService, times(0)).createVm(any(ProvisionVMRequest.class));
    }

    @Test
    public void provisionVmPorvisionFailsTest() throws InterruptedException {

        vmActionAfter.state = "FAILED";
        vmActionInProgress.tickNum = 3;

        Mockito.when(vmService.createVm(action.hfsProvisionRequest)).thenReturn(vmActionInProgress);
        Mockito.when(vmService.getVmAction(vmActionInProgress.vmId, vmActionInProgress.vmActionId)).thenReturn(vmActionAfter);
        Mockito.when(vmService.getVm(vmActionAfter.vmId)).thenReturn(vm);

        Mockito.when(hfsNetworkSerivce.acquireIp(action.project.getVhfsSgid())).thenReturn(addressAction);
        Mockito.when(hfsNetworkSerivce.getAddress(addressAction.addressId)).thenReturn(ip);

        runProvisionVmWorker();

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        assertEquals(vmActionAfter.vmId, action.vm.vmId);
        assertEquals(ActionStatus.ERROR, action.status);
        assertEquals(ip.addressId, action.ip.addressId);
        assertEquals(CreateVmStep.ConfiguringServer, action.step);

        verify(vmService, times(1)).createVm(any(ProvisionVMRequest.class));
        verify(hfsNetworkSerivce, times(1)).acquireIp(action.project.getVhfsSgid());
        verify(hfsNetworkSerivce, times(0)).bindIp(ip.addressId, vmActionAfter.vmId);
    }

    @Test
    public void provisionVmBindFailsTest() throws InterruptedException {

        AddressAction addressActionFailed = new AddressAction();
        addressActionFailed.status = Status.FAILED;
        addressActionFailed.addressId = ip.addressId;

        Mockito.when(vmService.createVm(action.hfsProvisionRequest)).thenReturn(vmActionInProgress);
        Mockito.when(vmService.getVmAction(vmActionInProgress.vmId, vmActionInProgress.vmActionId)).thenReturn(vmActionAfter);
        Mockito.when(vmService.getVm(vmActionAfter.vmId)).thenReturn(vm);

        Mockito.when(hfsNetworkSerivce.acquireIp(action.project.getVhfsSgid())).thenReturn(addressAction);
        Mockito.when(hfsNetworkSerivce.getAddress(addressAction.addressId)).thenReturn(ip);
        Mockito.when(hfsNetworkSerivce.bindIp(ip.addressId, vmActionInProgress.vmId)).thenReturn(addressActionFailed);

        runProvisionVmWorker();

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        assertEquals(vmActionAfter.vmId, action.vm.vmId);
        assertEquals(ActionStatus.ERROR, action.status);
        assertEquals(ip.addressId, action.ip.addressId);
        assertEquals(CreateVmStep.ConfiguringNetwork, action.step);

        verify(vmService, times(1)).createVm(any(ProvisionVMRequest.class));
        verify(hfsNetworkSerivce, times(1)).acquireIp(action.project.getVhfsSgid());
        verify(hfsNetworkSerivce, times(1)).bindIp(ip.addressId, vmActionAfter.vmId);
    }
}
