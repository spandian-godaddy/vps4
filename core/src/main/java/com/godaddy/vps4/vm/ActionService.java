package com.godaddy.vps4.vm;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.jdbc.ResultSubset;

public interface ActionService {

    long createAction(UUID resourceId, ActionType actionType, String request, String initiatedBy);

    Action getAction(long actionId);

    Action getAction(UUID resourceId, long actionId);

    default List<Action> getActions(UUID resourceId) {
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    ResultSubset<Action> getActions(UUID resourceId, long limit, long offset);

    ResultSubset<Action> getActions(UUID resourceId, long limit, long offset, ActionType actionType);

    ResultSubset<Action> getActions(UUID resourceId, long limit, long offset, List<String> statusList);

    ResultSubset<Action> getActions(UUID resourceId, long limit, long offset, List<String> statusList, Date beginDate, Date endDate);

    void tagWithCommand(long actionId, UUID commandId);

    void markActionInProgress(long actionId);

    void updateActionState(long actionId, String state);

    void completeAction(long actionId, String response, String notes);

    void failAction(long actionId, String response, String notes);

    void cancelAction(long actionId, String response, String notes);
}
