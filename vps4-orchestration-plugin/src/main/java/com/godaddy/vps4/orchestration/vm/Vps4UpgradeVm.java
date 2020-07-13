package com.godaddy.vps4.orchestration.vm;

import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField.PLAN_CHANGE_PENDING;

import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.RestoreVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4UpgradeVm",
        requestType=Vps4UpgradeVm.Request.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4UpgradeVm extends ActionCommand<Vps4UpgradeVm.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4UpgradeVm.class);

    private final VirtualMachineService virtualMachineService;
    private final SnapshotService snapshotService;
    private final VmUserService vmUserService;
    private final ProjectService projectService;
    private final ActionService snapshotActionService;
    private final CreditService creditService;
    private CommandContext context;
    private Request request;
    private UUID orionGuid;

    @Inject
    public Vps4UpgradeVm(@SnapshotActionService ActionService snapshotActionService,
                         ActionService actionService,
                         VirtualMachineService virtualMachineService,
                         SnapshotService snapshotService,
                         VmUserService vmUserService,
                         ProjectService projectService,
                         CreditService creditService) {
        super(actionService);
        this.snapshotActionService = snapshotActionService;
        this.virtualMachineService = virtualMachineService;
        this.snapshotService = snapshotService;
        this.vmUserService = vmUserService;
        this.projectService = projectService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.request = request;
        VirtualMachine originalVm = getOriginalVm();
        orionGuid = originalVm.orionGuid;

        stopVm(originalVm.hfsVmId);
        try {
            UUID snapshotId = createSnapshotForUpgrade(originalVm);
            createUpgradedVmFromSnapshot(snapshotId, originalVm);
        } catch (RuntimeException e) {
            // On failure, start the original VM back up
            logger.warn("Upgrade failed to complete, re-starting VM {}", request.vmId);
            startVm(originalVm.hfsVmId);
            throw e;
        }

        updateVmDetails();
        logger.info("Upgrade action complete for vm {}", request.vmId);
        return null;
    }

    private VirtualMachine getOriginalVm() {
        return context.execute("GetVmDetails",
                ctx -> virtualMachineService.getVirtualMachine(request.vmId), VirtualMachine.class);
    }

    private void stopVm(long hfsVmId) {
        context.execute(StopVm.class, hfsVmId);
    }

    private void startVm(long hfsVmId) {
        context.execute(StartVm.class, hfsVmId);
    }

    private UUID createSnapshotForUpgrade(VirtualMachine vm) {
        UUID snapshotId = createSnapshotRecord(vm.projectId, vm.vmId);
        long snapshotActionId = createSnapshotAction(snapshotId);
        snapshotVm(snapshotId, snapshotActionId, vm.hfsVmId);
        return snapshotId;
    }

    private UUID createSnapshotRecord(long projectId, UUID vmId) {
        return context.execute("CreateSnapshotRecord",
                ctx -> snapshotService.createSnapshot(projectId, vmId, request.autoBackupName,
                        SnapshotType.AUTOMATIC),
                UUID.class);
    }

    private long createSnapshotAction(UUID snapshotId) {
        return context.execute("CreateSnapshotAction",
                ctx -> snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT,
                        new JSONObject().toJSONString(), request.initiatedBy),
                long.class);
    }

    private void snapshotVm(UUID snapshotId, long actionId, long hfsVmId) {
        Vps4SnapshotVm.Request snapshotReq = new Vps4SnapshotVm.Request();
        snapshotReq.vps4SnapshotId = snapshotId;
        snapshotReq.orionGuid = orionGuid;
        snapshotReq.actionId = actionId;
        snapshotReq.hfsVmId = hfsVmId;
        snapshotReq.snapshotType = SnapshotType.AUTOMATIC;
        snapshotReq.shopperId = request.shopperId;
        snapshotReq.initiatedBy = request.initiatedBy;
        snapshotReq.allowRetries = false;

        logger.info("Creating snapshot-for-upgrade for VM {}", request.vmId);
        context.execute("Vps4SnapshotVm", Vps4SnapshotVm.class, snapshotReq);
    }

    private void createUpgradedVmFromSnapshot(UUID snapshotId, VirtualMachine vm) {
        // RestoreVm can be reused here as command creates a new vm from snapshot
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
        restoreVmInfo.orionGuid = orionGuid;
        restoreReq.restoreVmInfo = restoreVmInfo;
        restoreReq.actionId = request.actionId;
        restoreReq.privateLabelId = request.privateLabelId;

        logger.info("Creating upgraded-vm from snapshot for VM {}", request.vmId);
        context.execute("Vps4RestoreVm", Vps4RestoreVm.class, restoreReq);
    }

    private void updateVmDetails() {
        updateVmTierInDb();
        updateEcommCredit();
    }

    private void updateVmTierInDb() {
        logger.info("Updating tier to match new upgraded VM {}", request.vmId);

        int newSpecId = virtualMachineService.getSpec(request.newTier).specId;
        context.execute("UpdateVmTier", ctx -> {
            virtualMachineService.updateVirtualMachine(request.vmId, Collections.singletonMap("spec_id", newSpecId));
            return null;
        }, Void.class);
    }

    private void updateEcommCredit() {
        logger.info("Mark pending plan upgrade complete in credit for VM {}", request.vmId);
        creditService.updateProductMeta(orionGuid, PLAN_CHANGE_PENDING, "false");
    }


    public static class Request implements ActionRequest {
        public long actionId;
        public UUID vmId;
        public String shopperId;
        public String initiatedBy;
        public byte[] encryptedPassword;
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

}
