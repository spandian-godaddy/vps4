package com.godaddy.vps4.web.vm;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.network.AllocateIpWorker;
import com.godaddy.vps4.web.network.BindIpAction;
import com.godaddy.vps4.web.network.BindIpWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;

import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;

public class ProvisionVmWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProvisionVmWorker.class);

    final VmService vmService;

    final NetworkService hfsNetworkService;

    final CreateVmAction action;

    final CountDownLatch inProgressLatch = new CountDownLatch(1);

    final ExecutorService threadPool;

    final com.godaddy.vps4.network.NetworkService vps4NetworkService;
    
    final VirtualMachineService virtualMachineService;
    
    final UUID orionGuid;
    
    final String name;
    
    final long projectId;
    
    final int specId;
    
    final int managedLevel;
    
    final int imageId;

    public ProvisionVmWorker(VmService vmService, NetworkService hfsNetworkService, CreateVmAction action, ExecutorService threadPool,
            com.godaddy.vps4.network.NetworkService vps4NetworkService, VirtualMachineService virtualMachineService,
            UUID orionGuid, String name, long projectId, int specId,
            int managedLevel, int imageId) {
        this.vmService = vmService;
        this.hfsNetworkService = hfsNetworkService;
        this.action = action;
        this.threadPool = threadPool;
        this.vps4NetworkService = vps4NetworkService;
        this.virtualMachineService = virtualMachineService;
        this.orionGuid = orionGuid;
        this.name = name;
        this.projectId = projectId;
        this.specId = specId;
        this.managedLevel = managedLevel;
        this.imageId = imageId;
    }

    @Override
    public void run() {
        action.step = CreateVmStep.StartingServerSetup;
        logger.info("begin provision vm for request: {}", action.hfsProvisionRequest);
        action.status = ActionStatus.IN_PROGRESS;
        VmAction hfsCreateVmAction = null;

        IpAddress ip = allocatedIp();

        if (action.status != ActionStatus.ERROR) {

            hfsCreateVmAction = provisionVm(ip);

            if (hfsCreateVmAction != null) {
                action.vm = vmService.getVm(hfsCreateVmAction.vmId);
            }
        }

        if (action.status != ActionStatus.ERROR) {

            bindIp(ip, hfsCreateVmAction);
        }

        if (action.status != ActionStatus.ERROR){
            virtualMachineService.provisionVirtualMachine(action.vm.vmId, orionGuid, name, projectId, 
                                                        specId, managedLevel, imageId);
        }
        
        if (action.status != ActionStatus.ERROR) {
            action.step = CreateVmStep.SetupComplete;
            action.status = ActionStatus.COMPLETE;
        }

        logger.info("provision vm finished with status {} for action: {}", hfsCreateVmAction);
    }

    private void bindIp(IpAddress ip, VmAction hfsAction) {
        action.step = CreateVmStep.ConfiguringNetwork;
        try {
            BindIpAction bindIpAction = new BindIpAction(ip.addressId, hfsAction.vmId);
            new BindIpWorker(bindIpAction, hfsNetworkService).run();

            if (bindIpAction.status == ActionStatus.ERROR) {
                action.status = ActionStatus.ERROR;
                logger.warn("failed to bind addressId {} to vmId {}, action: {}", ip.addressId, hfsAction.vmId, bindIpAction);
            }
        }
        catch (Vps4Exception e) {
            action.status = ActionStatus.ERROR;
            logger.warn("failed to bind addressId {} to vmId {}", ip.addressId, hfsAction.vmId);
        }
    }

    private VmAction provisionVm(IpAddress ip) {

        logger.info("sending HFS VM request: {}", action.hfsProvisionRequest);

        action.step = CreateVmStep.GeneratingHostname;
        action.hfsProvisionRequest.hostname = HostnameGenerator.getHostname(ip.address);

        action.step = CreateVmStep.RequestingServer;
        VmAction hfsAction = vmService.createVm(action.hfsProvisionRequest);
        hfsAction = waitForVmAction(hfsAction);

        if (!hfsAction.state.equals("COMPLETE")) {
            logger.warn("failed to provision VM, action: {}", hfsAction);
            action.status = ActionStatus.ERROR;
        }
        return hfsAction;
    }

    private IpAddress allocatedIp() {
        action.step = CreateVmStep.RequestingIPAddress;

        IpAddress ip = null;
        try {
            ip = new AllocateIpWorker(hfsNetworkService, action.project, vps4NetworkService).call();
            action.ip = ip;
        }
        catch (Vps4Exception e) {
            action.status = ActionStatus.ERROR;
            logger.warn("failed to allocate an IP: {}", action);
        }
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

    protected VmAction waitForVmAction(VmAction hfsAction) {
        int currentHfsTick = 1;
        // wait for VmAction to complete
        while (hfsAction.state.equals("REQUESTED") || hfsAction.state.equals("IN_PROGRESS")) {

            logger.info("waiting on VM to provision: {}", hfsAction);

            if (hfsAction.state.equals("IN_PROGRESS")) {
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
