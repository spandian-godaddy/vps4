package com.godaddy.vps4.web.vm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.hfs.VmAction;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.network.AllocateIpWorker;
import com.godaddy.vps4.web.network.BindIpWorker;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.AddressAction.Status;
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

    public ProvisionVmWorker(VmService vmService, NetworkService hfsNetworkService, CreateVmAction action, ExecutorService threadPool,
            com.godaddy.vps4.network.NetworkService vps4NetworkService) {
        this.vmService = vmService;
        this.hfsNetworkService = hfsNetworkService;
        this.action = action;
        this.threadPool = threadPool;
        this.vps4NetworkService = vps4NetworkService;
    }

    @Override
    public void run() {

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

        if (action.status != ActionStatus.ERROR) {

            action.status = ActionStatus.COMPLETE;
        }

        logger.info("provision vm finished with status {} for action: {}", hfsCreateVmAction);
    }

    private void bindIp(IpAddress ip, VmAction hfsAction) {
        try {
            AddressAction bindIpAction = new BindIpWorker(hfsNetworkService, ip.addressId, hfsAction.vmId).call();

            if (bindIpAction.status == Status.FAILED) {
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

        action.hfsProvisionRequest.hostname = HostnameGenerator.getHostname(ip.address);
        VmAction hfsAction = vmService.createVm(action.hfsProvisionRequest);
        hfsAction = waitForVmAction(hfsAction);

        if (!hfsAction.state.equals("COMPLETE")) {
            logger.warn("failed to provision VM, action: {}", hfsAction);
            action.status = ActionStatus.ERROR;
        }

        return hfsAction;
    }

    private IpAddress allocatedIp() {
        Future<IpAddress> ipFuture = threadPool.submit(new AllocateIpWorker(hfsNetworkService, action.project, vps4NetworkService));

        IpAddress ip = null;
        try {
            ip = ipFuture.get();
            action.ip = ip;
        }
        catch (ExecutionException | InterruptedException e) {
            action.status = ActionStatus.ERROR;
            logger.warn("failed to allocate an IP: {}", action);
        }
        return ip;
    }

    protected VmAction waitForVmAction(VmAction hfsAction) {
        // wait for VmAction to complete
        while (hfsAction.state.equals("REQUESTED") || hfsAction.state.equals("IN_PROGRESS")) {

            logger.info("waiting on VM to provision: {}", hfsAction);

            if (hfsAction.state.equals("IN_PROGRESS")) {
                action.vm = vmService.getVm(hfsAction.vmId);
                inProgressLatch.countDown();
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
