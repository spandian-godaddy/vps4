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
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.vm.CreateVmStep;
import com.godaddy.vps4.web.vm.ProvisionVmWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;
import com.godaddy.vps4.web.vm.VmResource.ProvisionVmInfo;

import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.AddressAction.Status;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class ProvisionVmWorkerTest {

    private VmService vmService;
    private NetworkService hfsNetworkSerivce;
    private SysAdminService sysAdminService;
    private ExecutorService threadPool;
    private com.godaddy.vps4.network.NetworkService vps4NetworkService;
    private CPanelService cPanelService;

    private IpAddress ip;
    private VmAction vmActionInProgress;
    private VmAction vmActionAfter;
    private CreateVmAction action;
    private SysAdminAction sysAdminActionInProgress;
    private SysAdminAction sysAdminActionComplete;
    private Vm vm;
    private AddressAction addressAction;
    private VirtualMachineService virtualMachineService;
    private UUID orionGuid;
    private String name;
    private long projectId;
    private int specId;
    private int managedLevel;
    private String username;
    private Image image;

    @Before
    public void setup() {

        vmService = Mockito.mock(VmService.class);
        hfsNetworkSerivce = Mockito.mock(NetworkService.class);
        sysAdminService = Mockito.mock(SysAdminService.class);
        threadPool = Executors.newCachedThreadPool();
        vps4NetworkService = Mockito.mock(com.godaddy.vps4.network.NetworkService.class);
        cPanelService = Mockito.mock(CPanelService.class);
        virtualMachineService = Mockito.mock(VirtualMachineService.class);

        orionGuid = UUID.randomUUID();
        name = "testName";
        projectId = 1;
        specId = 1;
        managedLevel = 1;
        username = "testuser";

        ip = new IpAddress();
        ip.address = "127.0.0.1";
        ip.addressId = 123;
        ip.status = IpAddress.Status.UNBOUND;

        vmActionInProgress = new VmAction();
        vmActionInProgress.state = "IN_PROGRESS";
        vmActionInProgress.vmId = 12;
        vmActionInProgress.vmActionId = 1;
        vmActionInProgress.tickNum = 3;

        sysAdminActionInProgress = new SysAdminAction();
        sysAdminActionInProgress.status = SysAdminAction.Status.IN_PROGRESS;
        sysAdminActionInProgress.vmId = 12;
        sysAdminActionInProgress.sysAdminActionId = 123321;

        sysAdminActionComplete = new SysAdminAction();
        sysAdminActionComplete.status = SysAdminAction.Status.COMPLETE;
        sysAdminActionComplete.vmId = 12;
        sysAdminActionComplete.sysAdminActionId = 123321;

        vmActionAfter = new VmAction();
        vmActionAfter.state = "COMPLETE";
        vmActionAfter.vmId = vmActionInProgress.vmId;
        vmActionAfter.vmActionId = vmActionInProgress.vmActionId;

        image = new Image();
        image.controlPanel = ControlPanel.NONE;

        ProvisionVMRequest hfsRequest = new ProvisionVMRequest();
        hfsRequest.username = username;
        action = new CreateVmAction(hfsRequest);
        action.status = ActionStatus.IN_PROGRESS;
        Project project = new Project(1, "testProject", "vps4-1", 1, Instant.now(), Instant.MAX);
        action.project = project;

        vm = new Vm();
        vm.vmId = vmActionAfter.vmId;

        addressAction = new AddressAction();
        addressAction.status = Status.COMPLETE;
        addressAction.addressId = ip.addressId;
    }

    private void runProvisionVmWorker() {
        ProvisionVmInfo vmInfo = new ProvisionVmInfo(orionGuid, name, projectId, specId, managedLevel, image);
        ProvisionVmWorker worker = new ProvisionVmWorker(vmService, hfsNetworkSerivce, sysAdminService,
                vps4NetworkService, virtualMachineService, cPanelService, action, threadPool, vmInfo);
        worker.run();
    }

    private void runProvisionVmWorkerSuccessfully() throws InterruptedException {
        Mockito.when(vmService.createVm(action.hfsProvisionRequest)).thenReturn(vmActionInProgress);
        Mockito.when(vmService.getVmAction(vmActionInProgress.vmId, vmActionInProgress.vmActionId)).thenReturn(vmActionAfter);
        Mockito.when(vmService.getVm(vmActionAfter.vmId)).thenReturn(vm);

        Mockito.when(hfsNetworkSerivce.acquireIp(action.project.getVhfsSgid())).thenReturn(addressAction);
        Mockito.when(hfsNetworkSerivce.getAddress(addressAction.addressId)).thenReturn(ip);
        Mockito.when(hfsNetworkSerivce.bindIp(ip.addressId, vmActionInProgress.vmId)).thenReturn(addressAction);

        Mockito.when(sysAdminService.enableAdmin(vm.vmId, username)).thenReturn(sysAdminActionInProgress);
        Mockito.when(sysAdminService.disableAdmin(vm.vmId, username)).thenReturn(sysAdminActionInProgress);
        Mockito.when(sysAdminService.getSysAdminAction(sysAdminActionInProgress.sysAdminActionId)).thenReturn(sysAdminActionComplete);

        runProvisionVmWorker();

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    @Test
    public void provisionVmTest() throws InterruptedException {

        runProvisionVmWorkerSuccessfully();

        assertEquals(vmActionAfter.vmId, action.vm.vmId);
        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals(ip.addressId, action.ip.addressId);
        assertEquals(CreateVmStep.SetupComplete, action.step);

        verify(vmService, times(1)).createVm(any(ProvisionVMRequest.class));
        verify(hfsNetworkSerivce, times(1)).acquireIp(action.project.getVhfsSgid());
        verify(hfsNetworkSerivce, times(1)).bindIp(ip.addressId, vmActionAfter.vmId);
        verify(sysAdminService, times(1)).enableAdmin(vm.vmId, username);
        verify(sysAdminService, times(1)).disableAdmin(vm.vmId, username);
    }

    @Test
    public void provisionVmTestUnmanaged() throws InterruptedException {
        // Unmanaged should not disable admin access on provision.
        managedLevel = 0;

        runProvisionVmWorkerSuccessfully();

        verify(sysAdminService, times(1)).enableAdmin(vm.vmId, username);
        verify(sysAdminService, times(0)).disableAdmin(vm.vmId, username);
    }

    @Test
    public void disableAdminAccessFails() throws InterruptedException {
        sysAdminActionComplete.status = SysAdminAction.Status.FAILED;
        runProvisionVmWorkerSuccessfully();
        assertEquals(ActionStatus.ERROR, action.status);
    }

    @Test
    public void enableAdminAccessFails() throws InterruptedException {
        managedLevel = 0;
        sysAdminActionComplete.status = SysAdminAction.Status.FAILED;
        runProvisionVmWorkerSuccessfully();
        assertEquals(ActionStatus.ERROR, action.status);
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
    public void provisionVmProvisionFailsTest() throws InterruptedException {

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

    @Test
    public void provisionCPanelVmTest() throws InterruptedException {

        CPanelAction cpanelActionInProgress = new CPanelAction();
        cpanelActionInProgress.status = CPanelAction.Status.IN_PROGRESS;
        cpanelActionInProgress.actionId = 123456;

        CPanelAction cpanelActionComplete = new CPanelAction();
        cpanelActionComplete.status = CPanelAction.Status.COMPLETE;

        Mockito.when(cPanelService.imageConfig(vmActionInProgress.vmId, ip.address)).thenReturn(cpanelActionInProgress);
        Mockito.when(cPanelService.getAction(cpanelActionInProgress.actionId)).thenReturn(cpanelActionComplete);

        image.controlPanel = ControlPanel.CPANEL;

        runProvisionVmWorkerSuccessfully();

        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals(CreateVmStep.SetupComplete, action.step);

        verify(cPanelService, times(1)).imageConfig(vmActionInProgress.vmId, ip.address);
    }

    @Test
    public void provisionCPanelVmConfigFailsTest() throws InterruptedException {

        CPanelAction cpanelActionInProgress = new CPanelAction();
        cpanelActionInProgress.status = CPanelAction.Status.FAILED;

        Mockito.when(cPanelService.imageConfig(vmActionInProgress.vmId, ip.address)).thenReturn(cpanelActionInProgress);

        image.controlPanel = ControlPanel.CPANEL;

        runProvisionVmWorkerSuccessfully();

        assertEquals(ActionStatus.ERROR, action.status);
        assertEquals(CreateVmStep.ConfiguringCPanel, action.step);

        verify(cPanelService, times(1)).imageConfig(vmActionInProgress.vmId, ip.address);
    }
}
