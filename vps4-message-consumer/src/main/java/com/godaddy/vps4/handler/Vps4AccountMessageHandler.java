package com.godaddy.vps4.handler;

import static com.godaddy.vps4.handler.MessageNotificationType.ADDED;
import static com.godaddy.vps4.handler.util.Commands.execute;
import static com.godaddy.vps4.handler.util.Utils.isDBError;
import static com.godaddy.vps4.handler.util.Utils.isOrchEngineDown;
import static com.godaddy.vps4.handler.util.Utils.isVps4ApiDown;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.orchestration.vm.Vps4PlanChange;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.client.VmService;
import com.godaddy.vps4.web.client.VmShopperMergeService;
import com.godaddy.vps4.web.client.VmSuspendReinstateService;
import com.godaddy.vps4.web.client.VmZombieService;
import com.godaddy.vps4.web.vm.VmShopperMergeResource.ShopperMergeRequest;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;

public class Vps4AccountMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4AccountMessageHandler.class);

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final ActionService vmActionService;
    private final CommandService commandService;
    private final boolean processFullyManagedEmails;
    private final boolean primaryMessageConsumerServer;
    private final VmZombieService vmZombieService;
    private final VmShopperMergeService vmShopperMergeService;
    private final Vps4MessagingService messagingService;
    private final VmSuspendReinstateService vmSuspendReinstateService;
    private final VmService vmService;
    private final Config config;
    @Inject
    public Vps4AccountMessageHandler(
            VirtualMachineService virtualMachineService,
            CreditService creditService,
            ActionService vmActionService,
            CommandService commandService,
            Vps4MessagingService messagingService,
            VmZombieService vmZombieService,
            VmSuspendReinstateService vmSuspendReinstateService,
            VmService vmService,
            VmShopperMergeService vmShopperMergeService,
            Config config) {

        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.vmActionService = vmActionService;
        this.commandService = commandService;
        this.messagingService = messagingService;
        this.vmZombieService = vmZombieService;
        this.vmSuspendReinstateService = vmSuspendReinstateService;
        this.vmService = vmService;
        this.vmShopperMergeService = vmShopperMergeService;
        this.config = config;
        processFullyManagedEmails = Boolean.parseBoolean(config.get("vps4MessageHandler.processFullyManagedEmails"));
        primaryMessageConsumerServer =
                Boolean.parseBoolean(config.get("vps4MessageHandler.primaryMessageConsumerServer"));
    }

    @Override
    public void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        logger.info("Consumed message: {} ", message.value());
        Vps4AccountMessage vps4Message = new Vps4AccountMessage(message);
        if (vps4Message.notificationType == MessageNotificationType.UNSUPPORTED) {
            logger.info("Message type is not supported, message processing will not continue for message {}", message);
            return;
        }

        VirtualMachineCredit credit = getCredit(vps4Message.accountGuid);
        if (credit == null) {
            logger.info("Account {} not found, message handling will not continue", vps4Message.accountGuid);
            return;
        }
        VirtualMachine vm = getVirtualMachine(credit);
        if (vm == null) {
            logger.info("Could not find an active virtual machine with orion guid {}", credit.getOrionGuid());
        }

        handleMessageByNotificationType(vps4Message, credit, vm);
    }

    private VirtualMachineCredit getCredit(UUID accountGuid) throws MessageHandlerException {
        VirtualMachineCredit credit = null;
        try {
            credit = creditService.getVirtualMachineCredit(accountGuid);
        } catch (Exception ex) {
            handleExceptionDuringAccountLookup(accountGuid, ex);
        }
        return credit;
    }

    private void handleExceptionDuringAccountLookup(UUID accountGuid, Exception ex) throws MessageHandlerException {
        logger.error("Error while trying to locate credit for account guid {}", accountGuid);
        boolean shouldRetry = isOrchEngineDown(ex) || isDBError(ex);
        throw new MessageHandlerException(shouldRetry, ex);
    }

    private VirtualMachine getVirtualMachine(VirtualMachineCredit credit) throws MessageHandlerException {
        VirtualMachine vm = null;
        try {
            if (credit.getProductId() != null) {
                vm = virtualMachineService.getVirtualMachine(credit.getProductId());
            }
        } catch (Exception ex) {
            handleExceptionDuringVmLookup(credit.getOrionGuid(), ex);
        }
        return vm;
    }

    private void handleExceptionDuringVmLookup(UUID accountGuid, Exception ex) throws MessageHandlerException {
        logger.error("Error while trying to locate server associated with account guid {}", accountGuid);
        boolean shouldRetry = isOrchEngineDown(ex) || isDBError(ex);
        throw new MessageHandlerException(shouldRetry, ex);
    }

    private void handleMessageByNotificationType(Vps4AccountMessage vps4Message, VirtualMachineCredit credit,
                                                 VirtualMachine vm) throws MessageHandlerException {
        try {
            switch (vps4Message.notificationType) {
                case ABUSE_SUSPENDED:
                    abuseSuspendServer(vm);
                    break;
                case SUSPENDED:
                    billingSuspendServer(vm);
                    break;
                case ADDED:
                    setPurchasedAt(vps4Message, credit);
                    sendFullyManagedWelcomeEmail(credit);
                    processPlanChange(credit, vm);
                    break;
                case RENEWED:
                case UPDATED:
                    processPlanChange(credit, vm);
                    break;
                case REMOVED:
                    handleAccountCancellation(vm, credit);
                    break;
                case REINSTATED:
                    reinstateAccount(vm);
                    break;
                case SHOPPER_CHANGED:
                    processShopperMerge(vm, credit);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            handleMessageProcessingException(vps4Message, ex);
        }
    }

    private void abuseSuspendServer(VirtualMachine vm) {
        if (vm != null) {
            logger.info("Now performing an Abuse suspend on the server {}.", vm.vmId);
            vmSuspendReinstateService.abuseSuspendAccount(vm.vmId);
        }
    }

    private void billingSuspendServer(VirtualMachine vm) {
        if (vm != null) {
            logger.info("Now performing a Billing suspend on the server {}.", vm.vmId);
            vmSuspendReinstateService.billingSuspendAccount(vm.vmId);
        }
    }

    private void setPurchasedAt(Vps4AccountMessage vps4Message, VirtualMachineCredit credit) {
        if (isNewCredit(vps4Message, credit)) {
            setPurchasedAtInProductMeta(vps4Message);
        }
    }

    private boolean isNewCredit(Vps4AccountMessage vps4Message, VirtualMachineCredit credit) {
        return (primaryMessageConsumerServer && vps4Message.notificationType == ADDED
                && credit.getPurchasedAt() == null);
    }

    private void setPurchasedAtInProductMeta(Vps4AccountMessage vps4Message) {
        creditService.updateProductMeta(vps4Message.accountGuid, ProductMetaField.PURCHASED_AT,
                Instant.now().toString());
    }

    private void sendFullyManagedWelcomeEmail(VirtualMachineCredit credit) {
        if (credit.getAccountStatus() == AccountStatus.ACTIVE && processFullyManagedEmails
                && credit.isManaged() && !credit.isFullyManagedEmailSent()) {
            try {
                messagingService.sendFullyManagedEmail(credit.getShopperId(), credit.getControlPanel());
                creditService.updateProductMeta(credit.getOrionGuid(), ProductMetaField.FULLY_MANAGED_EMAIL_SENT,
                        "true");
            } catch (MissingShopperIdException | IOException e) {
                logger.warn("Failed to send fully managed welcome email", e);
            }
        }
    }

    private void processPlanChange(VirtualMachineCredit credit, VirtualMachine vm) {
        if (vm != null) {
            if (credit.getTier() != vm.spec.tier) {
                setPlanChangePendingInProductMeta(credit);
            }
            // abuse suspended vm's cannot be upgraded or renewed.
            if (isAccountAbuseSuspended(credit, vm)) {
                creditService.setStatus(vm.orionGuid, AccountStatus.ABUSE_SUSPENDED);
                return;
            }

            updateVmManagedLevel(credit, vm);
        }
    }

    private void setPlanChangePendingInProductMeta(VirtualMachineCredit credit) {
        creditService.updateProductMeta(credit.getOrionGuid(), ProductMetaField.PLAN_CHANGE_PENDING,
                String.valueOf(true));
    }

    private void updateVmManagedLevel(VirtualMachineCredit credit, VirtualMachine vm) {
        Vps4PlanChange.Request request = new Vps4PlanChange.Request();
        request.credit = credit;
        request.vm = vm;
        execute(commandService, vmActionService, "Vps4PlanChange", request);
    }

    private void handleAccountCancellation(VirtualMachine vm, VirtualMachineCredit credit) {
        if (vm != null) {
            logger.info("Vps4 account canceled: {}", credit.getOrionGuid());
            if (shouldTemporarilyRetainResources(credit)) {
                logger.info("Zombie'ing associated server {}", vm.vmId);
                this.vmZombieService.zombieVm(vm.vmId);
            } else {
                logger.info("Deleting server {}, will NOT zombie, has not been setup long enough", vm.vmId);
                this.vmService.destroyVm(vm.vmId);
            }
        }
    }

    private boolean shouldTemporarilyRetainResources(VirtualMachineCredit credit) {
        if (credit.getPurchasedAt() == null) {
            return true;
        }

        long minAccountAgeInDays = Long.valueOf(config.get("vps4.zombie.minimum.account.age"));
        Duration ageOfAccount = Duration.between(credit.getPurchasedAt(), Instant.now());
        return ageOfAccount.toDays() >= minAccountAgeInDays;
    }

    private void reinstateAccount(VirtualMachine vm) {
        if (vm != null) {
            // only billing suspended accounts are re-instated using the notification from
            // kafka messages
            logger.info("Now re-instating billing suspended account for vm: {} ", vm);
            vmSuspendReinstateService.reinstateBillingSuspendedAccount(vm.vmId);
        }
    }

    private boolean isAccountAbuseSuspended(VirtualMachineCredit credit, VirtualMachine vm) {
        if (credit.isAbuseSuspendedFlagSet()) {
            logger.warn(
                    "Cannot update an ABUSE SUSPENDED account. Account {}  for vm {} is abuse suspended. Account will"
                            + " need to be reinstated before any other actions can be performed.",
                    credit.getOrionGuid(), vm.vmId);
            return true;
        }
        return false;
    }

    private void processShopperMerge(VirtualMachine vm, VirtualMachineCredit credit) {
        if (vm != null) {
            ShopperMergeRequest shopperMergeRequest = new ShopperMergeRequest();
            shopperMergeRequest.newShopperId = credit.getShopperId();
            vmShopperMergeService.mergeShopper(vm.vmId, shopperMergeRequest);
        }
    }

    private void handleMessageProcessingException(Vps4AccountMessage vps4Message, Exception ex)
            throws MessageHandlerException {
        boolean shouldRetry;
        logger.error("Failed while handling message for account {} with exception {}", vps4Message.accountGuid, ex);

        switch (vps4Message.notificationType) {
            case UPDATED:
            case ADDED:
            case RENEWED:
                // These messages are handled by posting to orchestration engine
                shouldRetry = isOrchEngineDown(ex) || isDBError(ex);
                break;
            case ABUSE_SUSPENDED:
            case SUSPENDED:
            case REMOVED:
            case SHOPPER_CHANGED:
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
