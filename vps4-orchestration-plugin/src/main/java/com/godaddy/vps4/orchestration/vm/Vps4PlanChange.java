package com.godaddy.vps4.orchestration.vm;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.util.Monitoring;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.nodeping.CheckType;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;

@CommandMetadata(
        name="Vps4PlanChange",
        requestType=Vps4PlanChange.Request.class)
public class Vps4PlanChange implements Command<Vps4PlanChange.Request, Void>{

    private static final Logger logger = LoggerFactory.getLogger(Vps4PlanChange.class);
    private final VirtualMachineService virtualMachineService;
    private final NodePingService monitoringService;
    private final NetworkService networkService;
    private final Monitoring monitoring;
    
    @Inject
    public Vps4PlanChange(VirtualMachineService virtualMachineService,
            NodePingService monitoringService,
            NetworkService networkService,
            Monitoring monitoring) {
        this.virtualMachineService = virtualMachineService;
        this.monitoringService = monitoringService;
        this.networkService = networkService;
        this.monitoring = monitoring;
    }

    public static class Request extends VmActionRequest {
        public VirtualMachineCredit credit;
        public VirtualMachine vm;
        public int managedLevel;
    }

    @Override
    public Void execute(CommandContext context, Request req) {
        if(req.vm.managedLevel != req.credit.managedLevel) {
            logger.info("Processing managed level change for account {} to level {}", req.vm.vmId, req.credit.managedLevel);
            
            if(monitoring.hasFullyManagedMonitoring(req.credit)) {
                removeExistingMonitoringCheck(context, req);
                
                NodePingCheck check = addNewMonitoringCheck(context, req);

                addNewMonitoringCheckIdToIp(context, req, check);
            }

            updateVirtualMachineManagedLevel(context, req);
        }
        return null;
    }

    private void addNewMonitoringCheckIdToIp(CommandContext context, Request req, NodePingCheck check) {
        context.execute("AddCheckIdToIp-" + req.vm.primaryIpAddress.ipAddress, ctx -> {
            networkService.updateIpWithCheckId(req.vm.primaryIpAddress.ipAddressId, check.checkId);
            return null;
        }, Void.class);
    }

    private NodePingCheck addNewMonitoringCheck(CommandContext context, Request req) {
        CreateCheckRequest checkRequest = new CreateCheckRequest();
        checkRequest.target = req.vm.primaryIpAddress.ipAddress;
        checkRequest.label = req.vm.primaryIpAddress.ipAddress;
        checkRequest.interval = 1;
        checkRequest.type = CheckType.PING;
        checkRequest.webhookUrl = "http://www.godaddy.com";
        long fullyManagedMonitoringAccountId = monitoring.getAccountId(req.credit.managedLevel);
        NodePingCheck check = context.execute("CreateMonitoringCheckForVm-" + req.vm.vmId, 
                ctx -> monitoringService.createCheck(fullyManagedMonitoringAccountId, checkRequest), NodePingCheck.class);
        logger.info("Created monitoring check {} for vmId {}", check.checkId, req.vm.vmId);
        return check;
    }

    private void removeExistingMonitoringCheck(CommandContext context, Request req) {
        if(req.vm.primaryIpAddress.pingCheckId != null) {
            logger.info("Remove existing monitoring account {} from vmId {}", req.vm.primaryIpAddress.pingCheckId, req.vm.vmId);
            context.execute("DeleteMonitoringAccount-" + req.vm.primaryIpAddress.pingCheckId, 
                    ctx -> {monitoringService.deleteCheck(monitoring.getAccountId(req.vm), req.vm.primaryIpAddress.pingCheckId);
                    return null;
                    }, Void.class);
        }
    }

    private void updateVirtualMachineManagedLevel(CommandContext context, Request req) {
        Map<String, Object> paramsToUpdate = new HashMap<>();
        paramsToUpdate.put("managed_level", req.credit.managedLevel);
        context.execute("UpdateVmManagedLevel", ctx -> {
            virtualMachineService.updateVirtualMachine(req.credit.productId, paramsToUpdate);
            return null;
        }, Void.class);
    }
}
