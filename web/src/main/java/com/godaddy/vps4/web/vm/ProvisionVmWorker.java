package com.godaddy.vps4.web.vm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.cpanel.ImageConfigAction;
import com.godaddy.vps4.web.cpanel.ImageConfigWorker;
import com.godaddy.vps4.web.network.AllocateIpWorker;
import com.godaddy.vps4.web.network.BindIpAction;
import com.godaddy.vps4.web.network.BindIpWorker;
import com.godaddy.vps4.web.sysadmin.ToggleAdminWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;
import com.godaddy.vps4.web.vm.VmResource.ProvisionVmInfo;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class ProvisionVmWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProvisionVmWorker.class);

    final CreateVmAction action;
    final ExecutorService threadPool;

    final VmService vmService;
    final NetworkService hfsNetworkService;
    final SysAdminService sysAdminService;
    final VirtualMachineService virtualMachineService;
    final com.godaddy.vps4.network.NetworkService vps4NetworkService;

    final CountDownLatch inProgressLatch = new CountDownLatch(1);
    final ProvisionVmInfo vmInfo;
    final CPanelService cPanelService;

    private final String provisionFailedId = "PROVISION_VM_FAILED";

    public ProvisionVmWorker(VmService vmService, NetworkService hfsNetworkService, SysAdminService sysAdminService,
            com.godaddy.vps4.network.NetworkService vps4NetworkService, VirtualMachineService virtualMachineService,
            CPanelService cPanelService, CreateVmAction action, ExecutorService threadPool, ProvisionVmInfo vmInfo) {
        this.vmService = vmService;
        this.hfsNetworkService = hfsNetworkService;
        this.sysAdminService = sysAdminService;
        this.action = action;
        this.threadPool = threadPool;
        this.vps4NetworkService = vps4NetworkService;
        this.cPanelService = cPanelService;
        this.virtualMachineService = virtualMachineService;
        this.vmInfo = vmInfo;
    }

    @Override
    public void run() {
        action.step = CreateVmStep.StartingServerSetup;
        logger.info("begin provision vm for request: {}", action.hfsProvisionRequest);
        action.status = ActionStatus.IN_PROGRESS;
        VmAction hfsCreateVmAction = null;

        try {
            IpAddress ip = allocatedIp();

            hfsCreateVmAction = provisionVm(ip);

            if (hfsCreateVmAction != null) {
                action.vm = vmService.getVm(hfsCreateVmAction.vmId);
            }

            bindIp(ip, hfsCreateVmAction);

            virtualMachineService.provisionVirtualMachine(action.vm.vmId, vmInfo.orionGuid, vmInfo.name, vmInfo.projectId,
                    vmInfo.specId, vmInfo.managedLevel, vmInfo.image.imageId);

            if (vmInfo.image.controlPanel == ControlPanel.CPANEL) {
                runCPanelConfig(action.vm.vmId, ip.address);
            }

            setAdminAccess();

            action.step = CreateVmStep.SetupComplete;
            action.status = ActionStatus.COMPLETE;

            logger.info("provision vm finished with status {} for action: {}", hfsCreateVmAction);
        }
        catch (Vps4Exception e) {
            action.status = ActionStatus.ERROR;
            action.message = e.toString();
            logger.warn("Provision VM Failed: action={} exception={}", action.toString(), e.toString());
        }
    }

    private void runCPanelConfig(long vmId, String publicIp) {

        action.step = CreateVmStep.ConfiguringCPanel;

        ImageConfigAction imageConfigAction = new ImageConfigAction(vmId, publicIp);
        ImageConfigWorker imageConfigWorker = new ImageConfigWorker(imageConfigAction, cPanelService);
        imageConfigWorker.run();

        if (imageConfigAction.status == ActionStatus.ERROR) {
            throw new Vps4Exception(provisionFailedId, String.format("failed cpanel image config on vmId %d", vmId));
        }
    }

    private void bindIp(IpAddress ip, VmAction hfsAction) {
        action.step = CreateVmStep.ConfiguringNetwork;
        BindIpAction bindIpAction = new BindIpAction(ip.addressId, ip.address, hfsAction.vmId, IpAddressType.PRIMARY);
        new BindIpWorker(bindIpAction, hfsNetworkService, vps4NetworkService).run();

        if (bindIpAction.status == ActionStatus.ERROR) {
            throw new Vps4Exception(provisionFailedId, String.format("failed to bind ip, action: %s", bindIpAction));
        }
    }

    private VmAction provisionVm(IpAddress ip) {

        logger.info("sending HFS VM request: {}", action.hfsProvisionRequest);

        action.step = CreateVmStep.GeneratingHostname;
        action.hfsProvisionRequest.hostname = HostnameGenerator.getHostname(ip.address);

        action.step = CreateVmStep.RequestingServer;
        VmAction hfsAction = null;
        hfsAction = vmService.createVm(action.hfsProvisionRequest);

        if (hfsAction != null) {
            hfsAction = waitForVmAction(hfsAction);

            if (!(hfsAction.state == VmAction.Status.COMPLETE)) {
                throw new Vps4Exception(provisionFailedId, String.format("failed to provision VM, action: %s", hfsAction));
            }
        }
        return hfsAction;
    }

    private IpAddress allocatedIp() {
        action.step = CreateVmStep.RequestingIPAddress;

        IpAddress ip = new AllocateIpWorker(action.project, hfsNetworkService).call();
        action.ip = ip;
        return ip;
    }

    private static final Map<Integer, CreateVmStep> hfsTicks = newHfsTicksMap();

    static Map<Integer, CreateVmStep> newHfsTicksMap() {
        Map<Integer, CreateVmStep> hfsTicksMap = new ConcurrentHashMap<>();
        hfsTicksMap.put(1, CreateVmStep.RequestingServer);
        hfsTicksMap.put(2, CreateVmStep.CreatingServer);
        hfsTicksMap.put(3, CreateVmStep.ConfiguringServer);
        return hfsTicksMap;
    }

    private void setAdminAccess() {
        new ToggleAdminWorker(sysAdminService, action.vm.vmId, action.hfsProvisionRequest.username, vmInfo.managedLevel < 1).run();
    }

    protected VmAction waitForVmAction(VmAction hfsAction) {
        int currentHfsTick = 1;
        // wait for VmAction to complete
        while (hfsAction.state == VmAction.Status.NEW || hfsAction.state == VmAction.Status.REQUESTED
                || hfsAction.state == VmAction.Status.IN_PROGRESS) {

            logger.info("waiting on VM to provision: {}", hfsAction);

            if (hfsAction.state == VmAction.Status.IN_PROGRESS) {
                action.vm = vmService.getVm(hfsAction.vmId);
                inProgressLatch.countDown();
            }

            if (hfsAction.tickNum > currentHfsTick) {
                CreateVmStep newState = hfsTicks.get(hfsAction.tickNum);
                if (newState != null) {
                    action.step = newState;
                }
                currentHfsTick = hfsAction.tickNum;
            }

            // give the VM time to spin up
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
        }
        return hfsAction;
    }

    public void waitForVmId() {
        while (action.vm == null) {
            try {
                inProgressLatch.await(1, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted waiting for action");
            }
        }
    }

}
