package com.godaddy.vps4.vm;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.jdbc.ResultSubset;

public interface ActionService {

    long createAction(UUID vmId, ActionType actionType, String request, long userId);

    Action getAction(long actionId);

    Action getVmAction(UUID vmId, long actionId);
    
    ResultSubset<Action> getActions(UUID vmId, long limit, long offset);
    
    ResultSubset<Action> getActions(UUID vmId, long limit, long offset, List<String> statusList);
    
    void tagWithCommand(long actionId, UUID commandId);

    void markActionInProgress(long actionId);
    
    void updateActionState(long actionId, String state);

    void completeAction(long actionId, String response, String notes);

    void failAction(long actionId, String response, String notes);

}
