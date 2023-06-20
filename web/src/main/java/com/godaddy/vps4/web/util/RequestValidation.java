package com.godaddy.vps4.web.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.util.validators.Validator;
import com.godaddy.vps4.util.validators.ValidatorRegistry;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerType.Type;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;

public class RequestValidation {
    private static final long OPEN_SLOTS_PER_CREDIT = 1;
    static final int GODADDY_MAIL_RELAY_LIMIT = 10000;
    static final int BRAND_RESELLER_MAIL_RELAY_LIMIT = 25000;

    public static void validateServerIsActive(Vm vm) {
        if (!vm.status.equals("ACTIVE"))
            throw new Vps4Exception("INVALID_STATUS", "This action must be run while the server is running");
    }

    public static void validateServerIsActiveOrUnknown(Vm vm) {
        if (!Arrays.asList("ACTIVE", "UNKNOWN").contains(vm.status))
            throw new Vps4Exception("INVALID_STATUS", "This action must be run while the server is in active or unknown status");
    }

    public static void validateServerIsStoppedOrUnknown(Vm vm) {
        if (!Arrays.asList("STOPPED", "UNKNOWN").contains(vm.status))
            throw new Vps4Exception("INVALID_STATUS", "This action must be run while the server is in stopped or unknown status");
    }

    public static void validateNoConflictingActions(UUID vmId,
                                                    ActionService actionService,
                                                    ActionType... conflictingActions) {
        long numOfConflictingActions = actionService
                .getIncompleteActions(vmId)
                .stream()
                .filter(i -> Arrays.asList(conflictingActions).contains(i.type))
                .count();
        if (numOfConflictingActions > 0)
            throw new Vps4Exception("CONFLICTING_INCOMPLETE_ACTION", "VmAction is already running");
    }

    public static void validateServerIsDedicated(VirtualMachine vm) {
        if (vm.spec.isVirtualMachine()) {
            throw new Vps4Exception("INVALID_SERVER", "Only dedicated servers support this operation");
        }
    }

    public static void validateServerIsVirtual(VirtualMachine vm) {
        if (!vm.spec.isVirtualMachine()) {
            throw new Vps4Exception("INVALID_SERVER", "Only virtual servers support this operation");
        }
    }

    public static void validateIfSnapshotOverQuota(OhBackupDataService ohBackupDataService,
                                                   SnapshotService snapshotService,
                                                   VirtualMachine vm,
                                                   SnapshotType snapshotType) {
        long count = snapshotService.totalFilledSlots(vm.orionGuid, snapshotType);
        if (snapshotType.equals(SnapshotType.ON_DEMAND)
                && vm.spec.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING) {
            count += ohBackupDataService.totalFilledSlots(vm.vmId);
        }
        if (count > OPEN_SLOTS_PER_CREDIT) {
            throw new Vps4Exception("SNAPSHOT_OVER_QUOTA", "Snapshot creation rejected as quota exceeded");
        }
    }

    public static void validateNoOtherSnapshotsInProgress(OhBackupService ohBackupService,
                                                          SnapshotService snapshotService,
                                                          VirtualMachine vm) {
        boolean hasSnapshotInProgress = snapshotService.hasSnapshotInProgress(vm.orionGuid);
        boolean hasOhBackupInProgress = vm.spec.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING
                && !ohBackupService.getBackups(vm.vmId, OhBackupState.PENDING).isEmpty();
        if (hasSnapshotInProgress || hasOhBackupInProgress) {
            throw new Vps4Exception("SNAPSHOT_ALREADY_IN_PROGRESS",
                                    "Snapshot creation rejected as snapshot already in progress");
        }
    }

    public static void validateDcIdIsAllowed(int[] validDcIds, int dcId) {
        Arrays.stream(validDcIds)
              .filter(id -> id == dcId)
              .findFirst()
              .orElseThrow(() -> new Vps4Exception("INVALID_DC_ID", "That datacenter is not allowed"));
    }

    public static void validateUserIsShopper(GDUser user) {
        if (!user.isShopper())
            throw new Vps4NoShopperException();
    }

    public static void validateAgentIsOk(VirtualMachine vm, VmService vmService, TroubleshootVmService troubleshootVmService) {
        Vm hfsVm = vmService.getVm(vm.hfsVmId);
        if (hfsVm.status.equals("ACTIVE") && (!troubleshootVmService.getHfsAgentStatus(vm.hfsVmId).equals("OK"))) {
            throw new Vps4Exception("AGENT_DOWN", "Agent for vmId " + vm.vmId + " is down. Refusing to take snapshot.");
        }
    }

    public static void validateSnapshotName(String name) {
        Validator validator = ValidatorRegistry.getInstance().get("snapshot-name");
        if (!validator.isValid(name)) {
            throw new Vps4Exception("INVALID_SNAPSHOT_NAME", String.format("\"%s\" is an invalid snapshot name", name));
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

    public static void validateSnapshotNotPaused(SchedulerWebService schedulerWebService, UUID backupJobId, SnapshotType snapshotType) {
        if(snapshotType.equals(SnapshotType.AUTOMATIC) && schedulerWebService.getJob("vps4", "backups", backupJobId).isPaused){
            throw new Vps4Exception("AUTOMATIC_SNAPSHOTS_PAUSED", "Cannot take automatic snapshot while backup schedule is paused");
        }
    }

    public static void validateSnapshotExists(UUID snapshotId, Snapshot snapshot, GDUser user) {
        if (snapshot == null) {
            throw new NotFoundException("Unknown Snapshot ID: " + snapshotId);
        }

        // Allow admin user to access deleted Snapshots
        if (!user.isAdmin() && (snapshot.status == SnapshotStatus.DESTROYED ||
                snapshot.status == SnapshotStatus.CANCELLED ||
                snapshot.status == SnapshotStatus.ERROR_RESCHEDULED ||
                snapshot.status == SnapshotStatus.LIMIT_RESCHEDULED ||
                snapshot.status == SnapshotStatus.AGENT_DOWN)) {
            throw new Vps4Exception("SNAPSHOT_DELETED", String.format("The snapshot %s was DELETED", snapshot.id));
        }
    }

    public static void validateSnapshotBelongsToVm(UUID vmId, Snapshot snapshot) {
        if (!snapshot.vmId.equals(vmId)) {
            throw new Vps4Exception("VM_MISMATCH",
                                    String.format("Snapshot %s does not belong to VM %s", snapshot.id, vmId));
        }
    }

    public static void validatePassword(String password) {
        Validator validator = ValidatorRegistry.getInstance().get("password");
        if (!validator.isValid(password)){
            throw new Vps4Exception("INVALID_PASSWORD", String.format("%s is an invalid password", password));
        }
    }

    private static Validator getHostnameValidatorByImage(Image image) {
        if (image.controlPanel == ControlPanel.CPANEL) {
            return ValidatorRegistry.getInstance().get("cpanelHostname");
        }
        return ValidatorRegistry.getInstance().get("hostname");
    }

    public static void validateHostname(String hostname, Image image) {
        Validator validator = getHostnameValidatorByImage(image);
        if (!validator.isValid(hostname)){
            throw new Vps4Exception("INVALID_HOSTNAME", String.format("%s is an invalid hostname", hostname));
        }
    }

    private static boolean isBrandReseller(String resellerId, List<String> brandResellers) {
        return brandResellers.stream().anyMatch(rsId -> rsId.equals(resellerId));
    }

    public static void validateMailRelayUpdate(
            CreditService creditService, UUID orionGuid, GDUser gdUser, int newQuota, List<String> brandResellerIds) {
        VirtualMachineCredit vmCredit = creditService.getVirtualMachineCredit(orionGuid);

        if (gdUser.roles().contains(Role.HS_AGENT)) {
            if (isBrandReseller(vmCredit.getResellerId(), brandResellerIds)) {
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
                            ssoShopperId, vmCredit.getOrionGuid()));
        }

        return vmCredit;
    }

    public static void validateCreditIsNotInUse(VirtualMachineCredit credit) {
        if (!credit.isUsable())
            throw new Vps4Exception("CREDIT_ALREADY_IN_USE",
                    String.format("The virtual machine credit for orion guid %s is already provisioned'", credit.getOrionGuid()));
    }

    public static void validateDedResellerSelectedDc(DataCenterService dcService, String resellerId, int requestedDcId) {
        List<DataCenter> resellerDataCenters = dcService.getDataCentersByReseller(resellerId);
        // no data centers found for the reseller means the reseller is not restricted to any particular data center
        if (resellerDataCenters.size() > 0) {
            boolean resellerAllowsDc = resellerDataCenters.stream().anyMatch(dc -> dc.dataCenterId == requestedDcId);
            if (!resellerAllowsDc)
                throw new Vps4Exception("DATACENTER_UNSUPPORTED", "Data Center provided is not supported: " + requestedDcId);
        }
    }

    public static void validateRequestedImage(VirtualMachineCredit vmCredit, Image image) {
        String errMsg = "The image %s (%s:%s) is not valid for this credit.";

        ControlPanel creditCp = validateAndReturnEnumValue(ControlPanel.class, vmCredit.getControlPanel().toUpperCase());
        if (image.controlPanel != creditCp) {
            throw new Vps4Exception("INVALID_IMAGE",
                    String.format(errMsg, image.hfsName, "control panel", image.controlPanel));
        }

        OperatingSystem creditOs = validateAndReturnEnumValue(OperatingSystem.class, vmCredit.getOperatingSystem().toUpperCase());
        if (image.operatingSystem != creditOs) {
            throw new Vps4Exception("INVALID_IMAGE",
                    String.format(errMsg, image.hfsName, "os", image.operatingSystem));
        }

        Type creditType = vmCredit.isDed4() ? Type.DEDICATED : Type.VIRTUAL;
        if (image.serverType.serverType != creditType) {
            throw new Vps4Exception("INVALID_IMAGE",
                    String.format(errMsg, image.hfsName, "platform", image.serverType.serverType));
        }
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

    public static Instant validateAndReturnDateInstant(String dateToValidate) {
        Instant date = null;
        if(dateToValidate != null) {
            try {
                date = Instant.parse(dateToValidate);
            } catch (DateTimeParseException e) {
                throw new Vps4Exception("INVALID_PARAMETER", String.format("Date %s has invalid format, use ISO-8601 format such as 2011-12-03T10:15:30Z", dateToValidate));
            }
        }
        return date;
    }

    public static <E extends Enum<E>> E validateAndReturnEnumValue(Class<E> clazz, String name) {
        try {
            return Enum.valueOf(clazz, name);
        } catch(IllegalArgumentException ex) {
            throw new Vps4Exception("INVALID_PARAMETER", String.format("%s is an invalid %s", name, clazz.getSimpleName()));
        }
    }

    public static void validateServerPlatform(VirtualMachine vm, ServerType.Platform platform) {
        if (vm.spec.serverType.platform != platform) {
            throw new Vps4Exception("INVALID_PLATFORM",
                                    String.format("Operation is not permitted for \"%s\"",
                                                  vm.spec.serverType.platform));
        }
    }
}
