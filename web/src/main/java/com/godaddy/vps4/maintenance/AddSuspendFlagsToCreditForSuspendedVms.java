package com.godaddy.vps4.maintenance;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.log.LogModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Find all the suspended accounts in each datacenter and update the credit to include the suspend flags.
 */
public class AddSuspendFlagsToCreditForSuspendedVms {

    private static final Logger logger = LoggerFactory.getLogger(AddSuspendFlagsToCreditForSuspendedVms.class);
    private static boolean testMode = Boolean.parseBoolean(System.getProperty("testMode", String.valueOf(true)));
    private static Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new CreditModule(),
            new LogModule(),
            new HfsClientModule());

    @Inject
    public ActionService actionService;
    @Inject
    public CreditService creditService;
    @Inject
    public VirtualMachineService virtualMachineService;
    @Inject
    public DataSource dataSource;
    @Inject
    public Config config;

    public static void main(String[] args) {
        logger.info("************* TestMode is {} *************", testMode);

        AddSuspendFlagsToCreditForSuspendedVms addFlags = new AddSuspendFlagsToCreditForSuspendedVms();
        injector.injectMembers(addFlags);

        // get the list of suspended actions in the Datacenter.
        ActionService.ActionListFilters actionListFilters = new ActionService.ActionListFilters();
        actionListFilters.byStatus(ActionStatus.COMPLETE);
        actionListFilters.byType(Arrays.asList(ActionType.ABUSE_SUSPEND, ActionType.BILLING_SUSPEND));
        ResultSubset<Action> suspendedActions =
                addFlags.actionService.getActionList(actionListFilters);

        // get the vmIds associated with the suspended actions.
        Set<UUID> vmIdSet = new HashSet<>();
        if (suspendedActions != null) {
            suspendedActions.results.stream().map(suspendedAction -> vmIdSet.add(suspendedAction.resourceId))
                                    .collect(Collectors.toSet());
        }

        logger.info("Total Vm ids in suspend status: {}", vmIdSet.size());
        vmIdSet.stream().forEach(vmId -> logger.info("VmId: {}", vmId));

        // get the Orion GUID associated with each VM id
        Set<UUID> orionGuids = new HashSet<>();
        vmIdSet.stream().forEach(vmId -> orionGuids.add(addFlags.virtualMachineService.getOrionGuidByVmId(vmId)));
        orionGuids.stream().forEach(orionGuid -> logger.info("Orion Guid: {} ", orionGuid));

        for (UUID orionGuid : orionGuids) {
            VirtualMachineCredit credit;
            try {
                credit = addFlags.creditService.getVirtualMachineCredit(orionGuid);
                if (credit != null) {
                    logger.info("Credit for orionGuid {} is {}", orionGuid, credit.toString());
                }
            } catch (Exception ex) {
                logger.error("Encountered exception while trying to get credit for orionGuid {}", orionGuid);
                logger.error("Exception: {} ", ex);
                continue;
            }

            try {

                VirtualMachine virtualMachine = addFlags.virtualMachineService.getVirtualMachine(credit.getProductId());

                if (credit != null && addFlags.isActiveVirtualMachine(virtualMachine)) {
                    if (credit.getAccountStatus() == AccountStatus.ABUSE_SUSPENDED) {
                        if (!credit.isAbuseSuspendedFlagSet()) {
                            logger.info("Updating credit, set abuse_suspended flag for credit {}", orionGuid);
                            if (testMode == false) {
                                addFlags.creditService.setAbuseSuspendedFlag(virtualMachine.vmId, true);
                                addFlags.creditService.setBillingSuspendedFlag(virtualMachine.vmId, false);
                            }
                        }
                    } else if (credit.getAccountStatus() == AccountStatus.SUSPENDED) {
                        if (!credit.isBillingSuspendedFlagSet()) {
                            logger.info("Updating credit, set billing_suspended flag for vmId {}", virtualMachine.vmId);
                            if (testMode == false) {
                                addFlags.creditService.setBillingSuspendedFlag(virtualMachine.vmId, true);
                                addFlags.creditService.setAbuseSuspendedFlag(virtualMachine.vmId, false);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("Encountered exception in setting values for credit {}. Exception: {} ", orionGuid, ex);
                continue;
            }
        }
    }

    private boolean isActiveVirtualMachine(VirtualMachine virtualMachine) {
        if (virtualMachine == null) {
            logger.info("VirtualMachine is null");
            return false;
        }
        if (virtualMachine.canceled != null) {
            logger.info("Canceled date for vm is {}", LocalDateTime.ofInstant(virtualMachine.canceled, ZoneOffset.UTC));
            if (virtualMachine.canceled.isAfter(Instant.now(Clock.systemUTC()))) {
                if (virtualMachine.validUntil != null) {
                    logger.info("Valid until date for vm is {}", virtualMachine.validUntil.toString());
                    if (virtualMachine.validUntil.isAfter(Instant.now(Clock.systemUTC()))) {
                        logger.info("VirtualMachine vmId: {} is active", virtualMachine.vmId);
                        return true;
                    }
                }
            }
        }
        logger.info("VirtualMachine vmId: {} is not active", virtualMachine.vmId);
        return false;
    }
}

