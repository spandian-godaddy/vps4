package com.godaddy.vps4.web.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.validators.Validator;
import com.godaddy.vps4.util.validators.ValidatorRegistry;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;

import gdg.hfs.vhfs.vm.Vm;

public class RequestValidation {
    static final int GODADDY_MAIL_RELAY_LIMIT = 10000;
    static final int BRAND_RESELLER_MAIL_RELAY_LIMIT = 25000;

    public static void validateServerIsActive(Vm vm) {
        if (!vm.status.equals("ACTIVE"))
            throw new Vps4Exception("INVALID_STATUS", "This action must be run while the server is running");
    }

    public static void validateServerIsStopped(Vm vm) {
        if (!vm.status.equals("STOPPED"))
            throw new Vps4Exception("INVALID_STATUS", "This action must be run while the server is stopped");
    }

    public static void validateNoConflictingActions(UUID vmId,
                                                    ActionService actionService,
                                                    ActionType... conflictingActions) {
        List<String> actionList = new ArrayList<>();
        actionList.add("NEW");
        actionList.add("IN_PROGRESS");
        ResultSubset<Action> currentActions = actionService.getActions(
                vmId, -1, 0, actionList
        );

        if (currentActions != null) {
            long numOfConflictingActions = currentActions.results.stream()
                    .filter(i -> Arrays.asList(conflictingActions).contains(i.type)).count();
            if (numOfConflictingActions > 0)
                throw new Vps4Exception("CONFLICTING_INCOMPLETE_ACTION", "VmAction is already running");
        }
    }

    public static void validateIfSnapshotOverQuota(SnapshotService snapshotService, UUID orionGuid, SnapshotType snapshotType) {
        if (snapshotService.isOverQuota(orionGuid, snapshotType))
            throw new Vps4Exception("SNAPSHOT_OVER_QUOTA", "Snapshot creation rejected as quota exceeded");
    }

    public static void validateNoOtherSnapshotsInProgress(SnapshotService snapshotService, UUID orionGuid){
        if (snapshotService.otherBackupsInProgress(orionGuid)){
            throw new Vps4Exception("SNAPSHOT_ALREADY_IN_PROGRESS", "Snapshot creation rejected as snapshot already in progress");
        }
    }

    public static void validateUserIsShopper(GDUser user) {
        if (!user.isShopper())
            throw new Vps4NoShopperException();
    }

    public static void validateSnapshotName(String name) {
        Validator validator = ValidatorRegistry.getInstance().get("snapshot-name");
        if (!validator.isValid(name)) {
            throw new Vps4Exception("INVALID_SNAPSHOT_NAME", String.format("%s is an invalid snapshot name", name));
        }
    }

    public static void validateVmExists(UUID vmId, VirtualMachine virtualMachine, GDUser user) {
        // Default is to allow admin user to access deleted VMs
        validateVmExists(vmId, virtualMachine, user, true);
    }

    public static void validateVmExists(UUID vmId, VirtualMachine virtualMachine, GDUser user, boolean allowAdminOverride) {
        if (virtualMachine == null) {
            throw new NotFoundException("Unknown VM ID: " + vmId);
        }

        boolean adminOverride = user.isAdmin() && allowAdminOverride;
        if (virtualMachine.validUntil.isBefore(Instant.now()) && !adminOverride) {
            throw new Vps4Exception("VM_DELETED", String.format("The virtual machine %s was DELETED", virtualMachine.vmId));
        }
    }

    public static void validateSnapshotExists(UUID snapshotId, Snapshot snapshot, GDUser user) {
        if (snapshot == null) {
            throw new NotFoundException("Unknown Snapshot ID: " + snapshotId);
        }

        // Allow admin user to access deleted Snapshots
        if (!user.isAdmin() && (snapshot.status == SnapshotStatus.DESTROYED ||
                snapshot.status == SnapshotStatus.CANCELLED)) {
            throw new Vps4Exception("SNAPSHOT_DELETED", String.format("The snapshot %s was DELETED", snapshot.id));
        }
    }

    public static void validatePassword(String password) {
        Validator validator = ValidatorRegistry.getInstance().get("password");
        if (!validator.isValid(password)){
            throw new Vps4Exception("INVALID_PASSWORD", String.format("%s is an invalid password", password));
        }
    }

    private static boolean isBrandReseller(String resellerId) {
        String[] brandResellers = {
            "525848", // Heart Internet
            "525847", // Host Europe GmbH
            "525845", // Domain Factory GmbH
            "525844", // 123 Reg
            "495469"  // Media Temple
        };

        return Arrays.stream(brandResellers).anyMatch(rsId -> rsId.equals(resellerId));
    }

    public static void validateMailRelayUpdate(
            CreditService creditService, UUID orionGuid, GDUser gdUser, int newQuota) {
        VirtualMachineCredit vmCredit = creditService.getVirtualMachineCredit(orionGuid);

        if (gdUser.role().equals(Role.HS_AGENT)) {
            if (isBrandReseller(vmCredit.resellerId)) {
                if (newQuota > BRAND_RESELLER_MAIL_RELAY_LIMIT) {
                    throw new Vps4Exception(
                        "EXCEEDS_LIMIT",
                        String.format("New mail quota (%d) exceeds allowed limit. Limit is %d",
                            newQuota, BRAND_RESELLER_MAIL_RELAY_LIMIT));
                }
            }
            else {
                if (newQuota > GODADDY_MAIL_RELAY_LIMIT) {
                    throw new Vps4Exception(
                            "EXCEEDS_LIMIT",
                            String.format("New mail quota (%d) exceeds allowed limit. Limit is %d",
                                newQuota, GODADDY_MAIL_RELAY_LIMIT));
                }
            }
        }
    }

    public static VirtualMachineCredit getAndValidateUserAccountCredit(
            CreditService creditService, UUID orionGuid, String ssoShopperId) {

        VirtualMachineCredit vmCredit = creditService.getVirtualMachineCredit(orionGuid);
        if (vmCredit == null) {
            throw new Vps4Exception("CREDIT_NOT_FOUND",
                    String.format("The virtual machine credit for orion guid %s was not found", orionGuid));
        }

        if (vmCredit.isAccountSuspended()) {
            throw new Vps4Exception("ACCOUNT_SUSPENDED",
                    String.format("The virtual machine account for orion guid %s was SUSPENDED", orionGuid));
        }

        if (vmCredit.isAccountRemoved()) {
            throw new Vps4Exception("ACCOUNT_REMOVED", String.format("The virtual machine account for orion guid %s was REMOVED", orionGuid));
        }

        if (!vmCredit.isOwnedByShopper(ssoShopperId)) {
            throw new AuthorizationException(
                    String.format("Shopper %s does not have privilege for vm request with orion guid %s",
                            ssoShopperId, vmCredit.orionGuid));
        }

        return vmCredit;
    }

    public static void validateCreditIsNotInUse(VirtualMachineCredit credit) {
        if (!credit.isUsable())
            throw new Vps4Exception("CREDIT_ALREADY_IN_USE",
                    String.format("The virtual machine credit for orion guid %s is already provisioned'", credit.orionGuid));

    }

    public static void validateResellerCredit(DataCenterService dcService, String resellerId, int requestedDcId) {
        List<DataCenter> resellerDataCenters = dcService.getDataCentersByReseller(resellerId);
        // no data centers found for the reseller means the reseller is not restricted to any particular data center
        if (resellerDataCenters.size() > 0) {
            boolean resellerAllowsDc = resellerDataCenters.stream().anyMatch(dc -> dc.dataCenterId == requestedDcId);
            if (!resellerAllowsDc)
                throw new Vps4Exception("DATACENTER_UNSUPPORTED", "Data Center provided is not supported: " + requestedDcId);
        }
    }

    public static void verifyUserPrivilegeToProject(Vps4UserService userService, PrivilegeService privilegeService,
            String shopperId, long projectId) {
        Vps4User vps4User = userService.getUser(shopperId);
        if (vps4User == null) {
            throw new AuthorizationException(shopperId + " does not have privilege on service group " + projectId);
        }
        privilegeService.requireAnyPrivilegeToProjectId(vps4User, projectId);
    }

    public static void verifyUserPrivilegeToVm(Vps4UserService userService, PrivilegeService privilegeService,
            String shopperId, UUID vmId) {
        Vps4User vps4User = userService.getUser(shopperId);
        if (vps4User == null) {
            throw new AuthorizationException(shopperId + "does not have privilege for vm " + vmId);
        }
        privilegeService.requireAnyPrivilegeToVmId(vps4User, vmId);
    }

    public static void validateIfSnapshotExists(SnapshotService snapshotService, UUID snapshotId){
        if (snapshotService.getSnapshot(snapshotId) == null){
            throw new Vps4Exception("SNAPSHOT_NOT_FOUND", "Snapshot does not exist");
        }
    }

    public static void validateIfSnapshotIsLive(SnapshotService snapshotService, UUID snapshotId){
        if (snapshotService.getSnapshot(snapshotId).status != SnapshotStatus.LIVE){
            throw new Vps4Exception("SNAPSHOT_NOT_LIVE", "Snapshot is not LIVE");
        }
    }

    public static void validateIfSnapshotFromVm(VirtualMachineService vmService,
                                                SnapshotService snapshotService,
                                                UUID vmId, UUID snapshotId){
        if (!snapshotService.getSnapshot(snapshotId).vmId.equals(vmService.getVirtualMachine(vmId).vmId)){
            throw new Vps4Exception("SNAPSHOT_NOT_FROM_VM", "Snapshot is not from the vm");
        }
    }
}
