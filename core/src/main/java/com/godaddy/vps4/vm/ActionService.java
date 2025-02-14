package com.godaddy.vps4.vm;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.util.ActionListFilters;

public interface ActionService {

    List<ActionWithOrionGuid> getActionsForFailedPercentMonitor(int windowSize, int daysBack);

    long createAction(UUID resourceId, ActionType actionType, String request, String initiatedBy);

    Action getAction(long actionId);

    Action getAction(UUID resourceId, long actionId);

    default List<Action> getActions(UUID resourceId) {
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    long insertAction(UUID vmId, Action action);

    void tagWithCommand(long actionId, UUID commandId);

    void markActionInProgress(long actionId);

    void updateActionState(long actionId, String state);

    void completeAction(long actionId, String response, String notes);

    void failAction(long actionId, String response, String notes);

    void cancelAction(long actionId, String response, String notes);

    List<Action> getIncompleteActions(UUID resourceId);

    ResultSubset<Action> getActionList(ActionListFilters filters);

    List<Action> getIncompleteActions(int minimumAttempts, String action);

    List<ActionWithOrionGuid> getCreatesWithoutPanopta(long windowSize);

    List<String> getVmActionTypes(UUID vmId);
}
