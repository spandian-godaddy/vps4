package com.godaddy.vps4.handler;

import static com.godaddy.vps4.handler.util.Commands.execute;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
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
    private final boolean processFullyManagedEmails;
    private final Vps4MessagingService messagingService;
    private final int FULLY_MANAGED_LEVEL = 2;

    @Inject
    public Vps4MessageHandler(VirtualMachineService virtualMachineService,
            SnapshotService snapshotService,
            CreditService creditService,
            ActionService vmActionService,
            @SnapshotActionService ActionService snapshotActionService,
            CommandService commandService,
            Vps4MessagingService messagingService,
            Config config) {

        this.virtualMachineService = virtualMachineService;
        this.snapshotService = snapshotService;
        this.creditService = creditService;
        this.vmActionService = vmActionService;
        this.snapshotActionService = snapshotActionService;
        this.commandService = commandService;
        this.config = config;
        this.messagingService = messagingService;
        processFullyManagedEmails = Boolean.parseBoolean(config.get("vps4MessageHandler.processFullyManagedEmails"));

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

        VirtualMachineCredit credit;
        try {
            credit = creditService.getVirtualMachineCredit(vps4Message.accountGuid);
            if (credit == null) {
                logger.info("Account {} not found, message handling will not continue", vps4Message.accountGuid);
                return;
            }
        } catch (Exception ex) {
            logger.error("Unable to locate account credit for Virtual Machine using account guid {}", vps4Message.accountGuid);
            throw new MessageHandlerException(ex);
        }


        switch (credit.accountStatus) {
        case ABUSE_SUSPENDED:
        case SUSPENDED:
            stopVm(credit.productId);
            break;
        case ACTIVE:
            sendFullyManagedWelcomeEmail(credit);
            break;
        case REMOVED:
            destroyAccount(credit);
            break;
        default:
            break;
        }
    }

    private void sendFullyManagedWelcomeEmail(VirtualMachineCredit credit) {
        if (processFullyManagedEmails && credit.managedLevel == FULLY_MANAGED_LEVEL && !credit.fullyManagedEmailSent) {
            try {
                messagingService.sendFullyManagedEmail(credit.shopperId, credit.controlPanel);
                creditService.updateProductMeta(credit.orionGuid, ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");
            }
            catch (MissingShopperIdException | IOException e) {
                logger.warn("Failed to send fully managed welcome email", e);
            }
        }
    }

    private void destroyAccount(VirtualMachineCredit credit) throws MessageHandlerException {
        logger.info("Vps4 account cancelled: {} - Destroying vm and snapshots", credit.orionGuid);
        destroyVm(credit.productId);
        destroySnapshots(credit.orionGuid);
    }

    private void destroyVm(UUID vmId) throws MessageHandlerException {
        if (vmId == null) {
            logger.info("No existing vm found on credit or vm already destroyed");
            return;
        }

        try {
            VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
            long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);

            long actionId = vmActionService.createAction(vm.vmId, ActionType.DESTROY_VM,
                    new JSONObject().toJSONString(), vps4UserId);

            long pingCheckAccountId = Long.parseLong(config.get("nodeping.accountid"));

            Vps4DestroyVm.Request request = new Vps4DestroyVm.Request();
            request.hfsVmId = vm.hfsVmId;
            request.actionId = actionId;
            request.pingCheckAccountId = pingCheckAccountId;

            logger.info("Setting vm valid_until date to now.");
            virtualMachineService.destroyVirtualMachine(vm.hfsVmId);

            logger.info("Destroying vm: {} - actionId: {}", vmId, actionId);
            execute(commandService, vmActionService, "Vps4DestroyVm", request);

        } catch (Exception ex) {
            logger.error("Error: Could not destroy the vm for vmId: {}", vmId);
            throw new MessageHandlerException(ex);
        }

    }

    private void destroySnapshots(UUID orionGuid) throws MessageHandlerException {

        try {
            List<Snapshot> snapshots = snapshotService.getSnapshotsByOrionGuid(orionGuid)
                    .stream()
                    .filter(s -> s.status != SnapshotStatus.DESTROYED)
                    .collect(Collectors.toList());

            if (snapshots.isEmpty()) {
                logger.info("No snapshots found for account {}", orionGuid);
                return;
            }

            long vps4UserId = virtualMachineService.getUserIdByVmId(snapshots.get(0).vmId);

            for (Snapshot snapshot : snapshots) {
                long actionId = snapshotActionService.createAction(snapshot.id, ActionType.DESTROY_SNAPSHOT,
                        new JSONObject().toJSONString(), vps4UserId);

                Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
                request.hfsSnapshotId = snapshot.hfsSnapshotId;
                request.actionId = actionId;

                logger.info("Destroying snapshot: {} - actionId: {}", snapshot.id, actionId);
                execute(commandService, snapshotActionService, "Vps4DestroySnapshot", request);

            }
        } catch (Exception ex) {
            logger.error("Error: Could not perform destroy snapshots operation for account {}", orionGuid);
            throw new MessageHandlerException(ex);
        }

    }

    private void stopVm(UUID vmId) throws MessageHandlerException {
        if (vmId == null) {
            logger.info("No active vm found for credit");
            return;
        }

        try {
            VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
            long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
            long actionId = vmActionService.createAction(vmId, ActionType.STOP_VM,
                    new JSONObject().toJSONString(), vps4UserId);

            VmActionRequest request = new VmActionRequest();
            request.hfsVmId = vm.hfsVmId;
            request.actionId = actionId;

            logger.info("Stopping suspended vm: {} - actionId: {}", vmId, actionId);
            execute(commandService, vmActionService, "Vps4StopVm", request);

        } catch (Exception ex) {
            logger.error("Could not perform the stop VM operation for vmId {}.", vmId);
            throw new MessageHandlerException(ex);
        }
    }
}
