package com.godaddy.vps4.web.util;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Exception;
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
                throw new Vps4Exception("CONFLICTING_INCOMPLETE_ACTION", "Action is already running");
        }
    }
}
