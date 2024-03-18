package com.godaddy.vps4.orchestration.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.nydus.UpgradeNydus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import javax.inject.Inject;

@CommandMetadata(
        name="Vps4SyncOsStatus",
        requestType=VmActionRequest.class,
        responseType=Vps4SyncOsStatus.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4SyncOsStatus extends ActionCommand<VmActionRequest, Vps4SyncOsStatus.Response> {
    private final ActionService actionService;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;

    @Inject
    public Vps4SyncOsStatus(ActionService actionService, VirtualMachineService virtualMachineService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) {
        VmAction hfsAction = context.execute("Vps4SyncOsStatus",
                                             ctx -> vmService.syncOs(request.virtualMachine.hfsVmId, "osinfo"), VmAction.class);
        VmAction completeHfsAction = context.execute(WaitForManageVmAction.class, hfsAction);
        updateCurrentOsIfChanged(context, request.virtualMachine, completeHfsAction);

        Vps4SyncOsStatus.Response response = new Vps4SyncOsStatus.Response();
        response.hfsVmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    private void updateCurrentOsIfChanged(CommandContext context, VirtualMachine virtualMachine, VmAction hfsAction) {
        OsStatus status = readOsStatus(hfsAction);

        if (status.osUpgraded) {
            context.execute("UpdateCurrentOs", ctx -> {
                virtualMachineService.setCurrentOs(virtualMachine.vmId, status.operatingSystem);
                return null;
            }, Void.class);

            UpgradeNydus.Request request = new UpgradeNydus.Request();
            request.vmId = virtualMachine.vmId;
            request.hfsVmId = virtualMachine.hfsVmId;
            context.execute(UpgradeNydus.class, request);
        }
    }

    private OsStatus readOsStatus(VmAction hfsAction) {
        OsStatus status;
        try {
            status = mapper.readValue(hfsAction.resultset, OsStatus.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read OS information", e);
        }
        return status;
    }

    public static class Response {
        public long hfsVmId;
        public VmAction hfsAction;
    }

    public static class OsStatus {
        @JsonProperty("operating_system")
        public String operatingSystem;

        @JsonProperty("os_upgraded")
        public boolean osUpgraded;
    }
}
