package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.*;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CommandMetadata(
        name="Vps4UpgradeVm",
        requestType=Vps4UpgradeVm.Request.class,
        responseType=Vps4UpgradeVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4UpgradeVm extends ActionCommand<Vps4UpgradeVm.Request, Vps4UpgradeVm.Response> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4UpgradeVm.class);

    private final VirtualMachineService virtualMachineService;
    private final SnapshotService vps4SnapshotService;
    private final VmUserService vmUserService;
    private final ProjectService projectService;
    private final ActionService snapshotActionService;
    private UUID vps4VmId;
    private CommandContext context;
    private Request request;
    private CreditService creditService;

    @Inject
    public Vps4UpgradeVm(@SnapshotActionService ActionService snapshotActionService,
                         ActionService actionService,
                         VirtualMachineService virtualMachineService,
                         SnapshotService vps4SnapshotService,
                         VmUserService vmUserService,
                         ProjectService projectService,
                         CreditService creditService) {
        super(actionService);
        this.snapshotActionService = snapshotActionService;
        this.virtualMachineService = virtualMachineService;
        this.vps4SnapshotService = vps4SnapshotService;
        this.vmUserService = vmUserService;
        this.projectService = projectService;
        this.creditService = creditService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Request request) {
        this.request = request;
        this.context = context;
        this.vps4VmId = request.vmId;

        VirtualMachine vm = context.execute("GetVirtualMachine",
                ctx -> virtualMachineService.getVirtualMachine(vps4VmId),
                VirtualMachine.class);
        long oldHfsVmId = vm.hfsVmId;

        logger.info("creating upgrade snapshot for vm {}", request.vmId);
        Vps4SnapshotVm.Request snapshotReq = createVps4SnapshotVmRequest(oldHfsVmId, vm);
        context.execute("Vps4SnapshotVm", Vps4SnapshotVm.class, snapshotReq);

        logger.info("creating vm from snapshot for vm {}", request.vmId);
        Vps4RestoreVm.Request restoreReq = createVps4RestoreVmRequest(snapshotReq.vps4SnapshotId, vm);
        context.execute("Vps4RestoreVm", Vps4RestoreVm.class, restoreReq);

        logger.info("updating vm spec in db for vm {}", request.vmId);
        updateVirtualMachineTier();

        logger.info("closing the pending upgrade in ecomm for vm {}", request.vmId);
        creditService.updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.PLAN_CHANGE_PENDING, "false");

        logger.info("upgrade finished for vm {}", request.vmId);

        return null;
    }

    private Vps4SnapshotVm.Request createVps4SnapshotVmRequest(long oldHfsVmId, VirtualMachine vm){
        UUID snapshotId = context.execute("createSnapshot",
                ctx -> vps4SnapshotService.createSnapshot(vm.projectId, vm.vmId, request.autoBackupName,
                        SnapshotType.AUTOMATIC),
                UUID.class);
        long actionId = context.execute("createSnapshotAction",
                ctx -> snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT,
                        new JSONObject().toJSONString(), request.initiatedBy),
                long.class);
        Vps4SnapshotVm.Request snapshotReq = new Vps4SnapshotVm.Request();
        snapshotReq.hfsVmId = oldHfsVmId;  
        snapshotReq.vps4SnapshotId = snapshotId;
        snapshotReq.orionGuid = vm.orionGuid;
        snapshotReq.snapshotType = SnapshotType.AUTOMATIC;
        snapshotReq.shopperId = request.shopperId;
        snapshotReq.initiatedBy = request.initiatedBy;
        snapshotReq.actionId = actionId;
        logger.debug("snapshotReq = {}", snapshotReq);
        return snapshotReq;
    }

    private Vps4RestoreVm.Request createVps4RestoreVmRequest(UUID snapshotId, VirtualMachine vm){
    	Vps4RestoreVm.Request restoreReq = new Vps4RestoreVm.Request();
        RestoreVmInfo restoreVmInfo = new RestoreVmInfo();
        restoreVmInfo.vmId = request.vmId;
        restoreVmInfo.snapshotId = snapshotId;
        restoreVmInfo.sgid = projectService.getProject(vm.projectId).getVhfsSgid();
        restoreVmInfo.hostname = vm.hostname;
        restoreVmInfo.encryptedPassword = request.encryptedPassword;
        restoreVmInfo.rawFlavor = virtualMachineService.getSpec(request.newTier).specName;
        restoreVmInfo.username = vmUserService.getPrimaryCustomer(request.vmId).username;
        restoreVmInfo.zone = request.zone;
        restoreVmInfo.orionGuid = vm.orionGuid;
        restoreReq.restoreVmInfo = restoreVmInfo;
        restoreReq.actionId = request.actionId;
        restoreReq.privateLabelId = request.privateLabelId;
        logger.debug("restoreReq = {}", restoreReq);
        return restoreReq;
    }

    private void updateVirtualMachineTier() {
		  Map<String, Object> paramsToUpdate = new HashMap<>();
		  int newSpecId = virtualMachineService.getSpec(request.newTier).specId;
		  paramsToUpdate.put("spec_id", newSpecId);
		  context.execute("UpdateVmTier", ctx -> {
			  virtualMachineService.updateVirtualMachine(request.vmId, paramsToUpdate);
			  return null;
		  }, Void.class);
	}
	  
    public static class Request implements ActionRequest {
        public UUID vmId;
        public String shopperId;
        public String initiatedBy;
        public byte[] encryptedPassword;
        public long actionId;
        public int newTier;
        public String autoBackupName;
        public String zone;
        public String privateLabelId;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }

    public static class Response {
        public long vmId;
    }

}
