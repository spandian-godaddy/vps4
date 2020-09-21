package com.godaddy.vps4.orchestration.monitoring;

import javax.inject.Inject;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4AddMonitoring",
        requestType=VmActionRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4AddMonitoring extends ActionCommand<VmActionRequest, Void> {

    private final CreditService creditService;
    private CommandContext context;
    private VirtualMachine vm;

    @Inject
    public Vps4AddMonitoring(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;
        this.vm = request.virtualMachine;

        setupPanoptaAgent();
        removeNodePing();
        return null;
    }

    private void setupPanoptaAgent() {
        SetupPanopta.Request setupRequest = new SetupPanopta.Request();
        setupRequest.vmId = vm.vmId;
        setupRequest.orionGuid = vm.orionGuid;
        setupRequest.hfsVmId = vm.hfsVmId;
        setupRequest.shopperId = getShopperId();
        if (vm.primaryIpAddress != null) {
            setupRequest.fqdn = vm.primaryIpAddress.ipAddress;
        }
        context.execute(SetupPanopta.class, setupRequest);
    }

    private String getShopperId() {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        return credit.getShopperId();
    }

    private void removeNodePing() {
        IpAddress primaryIp = vm.primaryIpAddress;
        if (primaryIp != null && primaryIp.pingCheckId != null) {
            context.execute(RemoveNodePingMonitoring.class, primaryIp);
        }
    }

}
