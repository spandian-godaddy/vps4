package com.godaddy.vps4.handler;

import static com.godaddy.vps4.handler.util.Commands.execute;

import java.io.IOException;
import java.util.UUID;

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
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4PlanChange;
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
    private final CreditService creditService;
    private final ActionService vmActionService;
    private final CommandService commandService;
    private final boolean processFullyManagedEmails;
    private final Vps4MessagingService messagingService;
    private final int FULLY_MANAGED_LEVEL = 2;

    @Inject
    public Vps4MessageHandler(VirtualMachineService virtualMachineService,
            CreditService creditService,
            ActionService vmActionService,
            CommandService commandService,
            Vps4MessagingService messagingService,
            Config config) {

        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.vmActionService = vmActionService;
        this.commandService = commandService;
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
            processPlanChange(credit);
            break;
        case REMOVED:
            handleAccountCancellation(credit);
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

    private void processPlanChange(VirtualMachineCredit credit) {
        if(credit.productId != null) {
            VirtualMachine vm = virtualMachineService.getVirtualMachine(credit.productId);
            if(vm != null) {
                Vps4PlanChange.Request request = new Vps4PlanChange.Request();
                request.credit = credit;
                request.vm = vm;
                execute(commandService, vmActionService, "Vps4PlanChange", request);
            }
        }
    }

    private void handleAccountCancellation(VirtualMachineCredit credit) throws MessageHandlerException {
        logger.info("Vps4 account canceled: {} - queueing account cancellation command", credit.orionGuid);
        execute(commandService, "Vps4ProcessAccountCancellation", credit);
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
  