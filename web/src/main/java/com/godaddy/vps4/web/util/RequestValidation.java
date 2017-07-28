package com.godaddy.vps4.web.util;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.util.validators.Validator;
import com.godaddy.vps4.util.validators.ValidatorRegistry;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import gdg.hfs.vhfs.vm.Vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RequestValidation {
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

    public static void validateIfSnapshotOverQuota(SnapshotService snapshotService, UUID vmId) {
        if (snapshotService.isOverQuota(vmId))
            throw new Vps4Exception("SNAPSHOT_OVER_QUOTA", "Snapshot creation rejected as quota exceeded");
    }

    public static void ensureHasShopperAccess(GDUser user) {
        if (user.getShopperId() == null)
            throw new Vps4NoShopperException();
    }

    public static void validateSnapshotName(String name) {
        Validator validator = ValidatorRegistry.getInstance().get("snapshot-name");
        if (!validator.isValid(name)){
            throw new Vps4Exception("INVALID_SNAPSHOT_NAME", String.format("%s is an invalid snapshot name", name));
        }
    }

    public static void verifyUserPrivilegeToProject(
            Vps4UserService userService, PrivilegeService privilegeService, String shopperId, long projectId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(shopperId);
        privilegeService.requireAnyPrivilegeToProjectId(vps4User, projectId);
    }

    public static void verifyUserPrivilegeToVm(
            Vps4UserService userService, PrivilegeService privilegeService, String shopperId, UUID vmId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(shopperId);
        privilegeService.requireAnyPrivilegeToVmId(vps4User, vmId);
    }
}
