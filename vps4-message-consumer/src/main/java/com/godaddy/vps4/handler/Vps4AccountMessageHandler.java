package com.godaddy.vps4.handler;

import static com.godaddy.vps4.handler.util.Commands.execute;
import static com.godaddy.vps4.handler.util.Utils.isOrchEngineDown;
import static com.godaddy.vps4.handler.util.Utils.isDBError;
import static com.godaddy.vps4.handler.util.Utils.isVps4ApiDown;

import java.io.IOException;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.web.client.VmZombieService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;
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

public class Vps4AccountMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4AccountMessageHandler.class);

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final ActionService vmActionService;
    private final CommandService commandService;
    private final boolean processFullyManagedEmails;
    private final VmZombieService vmZombieService;
    private final Vps4MessagingService messagingService;
    private final int FULLY_MANAGED_LEVEL = 2;
    private final String ACCOUNT_MESSAGE_HANDLER_NAME = "AccountMessageHandler";

    @Inject
    public Vps4AccountMessageHandler(VirtualMachineService virtualMachineService,
            CreditService creditService,
            ActionService vmActionService,
            CommandService commandService,
            Vps4MessagingService messagingService,
            VmZombieService vmZombieService,
            Config config) {

        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.vmActionService = vmActionService;
        this.commandService = commandService;
        this.messagingService = messagingService;
        this.vmZombieService = vmZombieService;
        processFullyManagedEmails = Boolean.parseBoolean(config.get("vps4MessageHandler.processFullyManagedEmails"));
    }

    @Override
    public void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        logger.info("Consumed message: {} ", message.value());
        Vps4AccountMessage vps4Message = new Vps4AccountMessage(message);

        VirtualMachineCredit credit;
        VirtualMachine vm;
        try {
            credit = creditService.getVirtualMachineCredit(vps4Message.accountGuid);
            if (credit == null) {
                logger.info("Account {} not found, message handling will not continue", vps4Message.accountGuid);
                return;
            }

            if(credit.accountStatus == AccountStatus.ACTIVE) {
                // this is pulled out of the switch block because it needs to happen whether there is an existing
                // vm or not.  The sendFullyManagedWelcomeEmail method will check if this DC is
                // configured to send the email.
                sendFullyManagedWelcomeEmail(credit);
            }

            vm = getVirtualMachine(credit);
            if(vm == null) {
                return;
            }
        } catch (Exception ex) {
            logger.error(
                "Error while trying to locate account credit for Virtual Machine using account guid {}",
                vps4Message.accountGuid);
            boolean shouldRetry = isOrchEngineDown(ex) || isDBError(ex);
            throw new MessageHandlerException(shouldRetry, ex);
        }


        try {
            switch (credit.accountStatus) {
                case ABUSE_SUSPENDED:
                case SUSPENDED:
                    stopVm(vm);
                    break;
                case ACTIVE:
                    processPlanChange(credit, vm);
                    break;
                case REMOVED:
                    handleAccountCancellation(vm, credit);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            boolean shouldRetry;
            logger.error("Failed while handling message for account {} with exception {}", vps4Message.accountGuid, ex);

            switch (credit.accountStatus) {
                case ABUSE_SUSPENDED:
                case SUSPENDED:
                case ACTIVE:
                    // These messages are handled by posting to orchestration engine
                    shouldRetry = isOrchEngineDown(ex) || isDBError(ex);
                    break;
                case REMOVED:
                    // These messages are handled by posting to vps4 api
                    shouldRetry = isDBError(ex) || isVps4ApiDown(ex);
                    break;
                default:
                    shouldRetry = isDBError(ex);
                    break;
            }

            throw new MessageHandlerException(shouldRetry, ex);
        }
    }

    private void sendFullyManagedWelcomeEmail(VirtualMachineCredit credit) {
        // We can control which datacenter to send welcome emails from the config file variable assigned to processFullyManagedEmails.
        // We decided to only send these emails from one datacenter because there will be no vm or datacenter
        // associated with the credit when we want to send the email, and either all or no datacenters would send the
        // welcome email.

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

    private VirtualMachine getVirtualMachine(VirtualMachineCredit credit){
        VirtualMachine vm = null;
        if(credit.productId != null) {
            vm = virtualMachineService.getVirtualMachine(credit.productId);
        }
        if(vm == null) {
            logger.info("Could not find an active virtual machine with orion guid {}", credit.orionGuid);
        }
        return vm;
    }

    private void processPlanChange(VirtualMachineCredit credit, VirtualMachine vm) {
        if (credit.tier != vm.spec.tier) {
            // Update credit that a tier upgrade is pending.  Customer will initiate manually as it will incur down time.
            creditService.updateProductMeta(credit.orionGuid, ProductMetaField.PLAN_CHANGE_PENDING, "true");
        }

        // Updates the managed level of a vm
        Vps4PlanChange.Request request = new Vps4PlanChange.Request();
        request.credit = credit;
        request.vm = vm;
        execute(commandService, vmActionService, "Vps4PlanChange", request);
    }

    private void handleAccountCancellation(VirtualMachine vm, VirtualMachineCredit credit) {
        logger.info("Vps4 account canceled: {} - Zombie'ing associated vm", credit.orionGuid);
        this.vmZombieService.zombieVm(vm.vmId);
    }

    private void stopVm(VirtualMachine vm) {
        long actionId = vmActionService.createAction(vm.vmId, ActionType.STOP_VM,
                new JSONObject().toJSONString(), ACCOUNT_MESSAGE_HANDLER_NAME);

        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        request.actionId = actionId;

        logger.info("Stopping suspended vm: {} - actionId: {}", vm.vmId, actionId);
        execute(commandService, vmActionService, "Vps4StopVm", request);
    }
}
