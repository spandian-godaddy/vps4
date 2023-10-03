package com.godaddy.vps4.orchestration.vm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddMonitoring;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4MoveBack",
        requestType = VmActionRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4MoveBack extends ActionCommand<VmActionRequest, Void> {
    private final VirtualMachineService virtualMachineService;
    private final SchedulerWebService schedulerWebService;
    private final NetworkService networkService;
    private final CreditService creditService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveBack.class);

    private CommandContext context;
    private VmActionRequest request;

    @Inject
    public Vps4MoveBack(ActionService actionService,
                        VirtualMachineService virtualMachineService,
                        SchedulerWebService schedulerWebService,
                        NetworkService networkService,
                        CreditService creditService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.schedulerWebService = schedulerWebService;
        this.networkService = networkService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;
        this.request = request;

        VirtualMachine vm = request.virtualMachine;
        List<Long> addressIds = new ArrayList<>();
        addressIds.add(vm.primaryIpAddress.addressId);
        List<IpAddress> additionalIps = networkService.getAllVmSecondaryAddresses(vm.hfsVmId);
        addressIds.addAll(additionalIps.stream().map(ipAddress -> ipAddress.addressId).collect(Collectors.toList()));

        try {
            setVmCanceledAndValidUntil(request.virtualMachine.vmId);
            setIpsValidUntil(addressIds);
            resumeAutomaticBackups(vm.backupJobId);
            updateProdMeta(request.virtualMachine.dataCenter.dataCenterId, request.virtualMachine.vmId, vm.orionGuid);
            installPanopta();
        } catch (Exception e) {
            String errorMessage = String.format("Move back failed for VM %s", request.virtualMachine.vmId);
            logger.warn(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
        return null;
    }

    private void resumeAutomaticBackups(UUID backupJobId) {
        if (backupJobId != null) { // OH VMs will have a backupJobId of null, but we don't resume those snapshots ourselves.
            context.execute("ResumeAutomaticBackups", ctx -> {
                schedulerWebService.resumeJob("vps4", "backups", backupJobId);
                return null;
            }, Void.class);
        }
    }

    private void setVmCanceledAndValidUntil(UUID vmId) {
        context.execute("ClearVmCanceled", ctx -> {
            virtualMachineService.clearVmCanceled(vmId);
            return null;
        }, Void.class);

        context.execute("MarkVmAsActive", ctx -> {
            virtualMachineService.setVmActive(vmId);
            return null;
        }, Void.class);
    }

    private void setIpsValidUntil(List<Long> addressIds) {
        for (Long addressId : addressIds) {
            context.execute("MarkIpActive-" + addressId, ctx -> {
                networkService.activateIpAddress(addressId);
                return null;
            }, Void.class);
        }
    }

    private void updateProdMeta(int dcId, UUID vmId, UUID orionGuid) {
        Map<ECommCreditService.ProductMetaField, String> newProdMeta = new EnumMap<>(ECommCreditService.ProductMetaField.class);
        newProdMeta.put(ECommCreditService.ProductMetaField.DATA_CENTER, String.valueOf(dcId));
        newProdMeta.put(ECommCreditService.ProductMetaField.PRODUCT_ID, vmId.toString());
        context.execute("UpdateProdMeta", ctx -> {
            creditService.updateProductMeta(orionGuid, newProdMeta);
            return null;
        }, Void.class);
    }

    private void installPanopta() {
        try {
            context.execute(Vps4AddMonitoring.class, request);
        } catch (Exception e) {
            logger.error("Exception while setting up Panopta for migrated VM {}: {}", request.virtualMachine.vmId, e);
        }
    }
}
