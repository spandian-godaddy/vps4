package com.godaddy.vps4.vm;

public interface ActionService {

    long createAction(long vmId, ActionType actionType, String request, long userId);

    Action getAction(long actionId);

    void markActionInProgress(long actionId);
    
    void updateActionState(long actionId, String state);

    void completeAction(long actionId, String response, String notes);

    void failAction(long actionId, String response, String notes);

}
