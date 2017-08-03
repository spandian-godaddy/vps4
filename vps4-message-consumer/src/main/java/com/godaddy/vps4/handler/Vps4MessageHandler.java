package com.godaddy.vps4.handler;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.handler.util.Commands;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotWithDetails;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;

public class Vps4MessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4MessageHandler.class);

    private JSONParser parser = new JSONParser();

    private final VirtualMachineService virtualMachineService;
    private final SnapshotService snapshotService;
    private final CreditService creditService;
    private final ActionService vmActionService;
    private final ActionService snapshotActionService;
    private final CommandService commandService;
    private final Config config;
    private final long pingCheckAccountId;

    @Inject
    public Vps4MessageHandler(VirtualMachineService virtualMachineService,
            SnapshotService snapshotService,
            CreditService creditService,
            ActionService vmActionService,
            @Named("Snapshot_action") ActionService snapshotActionService,
            CommandService commandService,
            Config config) {
        this.virtualMachineService = virtualMachineService;
        this.snapshotService = snapshotService;
        this.creditService = creditService;
        this.vmActionService = vmActionService;
        this.snapshotActionService = snapshotActionService;
        this.commandService = commandService;
        this.config = config;

        pingCheckAccountId = Long.parseLong(this.config.get("nodeping.accountid"));
    }

    @Override
    public void handleMessage(String message) throws MessageHandlerException {
        logger.info("Consumed message: {} ", message);
        Vps4Message vps4Message;

        try {
            JSONObject obj = (JSONObject) parser.parse(message);
            vps4Message = new Vps4Message(obj);
        }
        catch (ParseException e) {
            throw new MessageHandlerException("Can't parse message JSON", e);
        }
        catch (IllegalArgumentException e) {
            throw new MessageHandlerException("Message values are the wrong type", e);
        }

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vps4Message.accountGuid);
        if (credit == null) {
            logger.info("Account {} not found, message handling will not continue", vps4Message.accountGuid);
            return;
        }

        switch (credit.accountStatus) {
        case ABUSE_SUSPENDED:
        case SUSPENDED:
            stopVm(credit.productId);
            break;
        case ACTIVE:
            // Do nothing. On reinstate customer will need to manually restart their VM.
            break;
        case REMOVED:
            destroyAccount(credit);
            break;
        default:
            break;
        }
    }

    private void destroyAccount(VirtualMachineCredit credit) {
        logger.info("Vps4 account cancelled: {} - Destroying vm and snapshots", credit.orionGuid);
        destroyVm(credit.productId);
        destroySnapshots(credit.orionGuid);
    }

    private void destroyVm(UUID vmId) {
        if (vmId == null) {
            logger.info("No existing vm found on credit or vm already destroyed");
            return;
        }

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);

        long actionId = vmActionService.createAction(vm.vmId, ActionType.DESTROY_VM,
                new JSONObject().toJSONString(), vps4UserId);

        Vps4DestroyVm.Request request = new Vps4DestroyVm.Request();
        request.hfsVmId = vm.hfsVmId;
        request.actionId = actionId;
        request.pingCheckAccountId = pingCheckAccountId;

        logger.info("Destroying vm: {} - actionId: {}", vmId, actionId);
        Commands.execute(commandService, vmActionService, "Vps4DestroyVm", request);
    }

    private void destroySnapshots(UUID orionGuid) {
        List<SnapshotWithDetails> snapshots = snapshotService.getSnapshotsByOrionGuid(orionGuid)
                .stream()
                .filter(s -> s.status != SnapshotStatus.DESTROYED)
                .collect(Collectors.toList());

        if (snapshots.isEmpty())
            return;

        long vps4UserId = virtualMachineService.getUserIdByVmId(snapshots.get(0).vmId);

        for (SnapshotWithDetails snapshot: snapshots) {
            long actionId = snapshotActionService.createAction(snapshot.id, ActionType.DESTROY_SNAPSHOT,
                    new JSONObject().toJSONString(), vps4UserId);

            Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
            request.hfsSnapshotId = snapshot.hfsSnapshotId;
            request.actionId = actionId;

            logger.info("Destroying snapshot: {} - actionId: {}", snapshot.id, actionId);
            Commands.execute(commandService, snapshotActionService, "Vps4DestroySnapshot", request);
        }

    }

    private void stopVm(UUID vmId) {
        if (vmId == null) {
            logger.info("No active vm found for credit");
            return;
        }

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        long actionId = vmActionService.createAction(vmId, ActionType.STOP_VM,
                new JSONObject().toJSONString(), vps4UserId);

        VmActionRequest request = new VmActionRequest();
        request.hfsVmId = vm.hfsVmId;
        request.actionId = actionId;

        logger.info("Stopping suspended vm: {} - actionId: {}", vmId, actionId);
        Commands.execute(commandService, vmActionService, "Vps4StopVm", request);
    }
}
