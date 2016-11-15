package com.godaddy.vps4.vm;

import java.util.Map;

public interface ActionService {
    long createAction(long vmId, String request, long userId);
    Action getAction(long actionId);
    void updateAction(long actionId, Map<String, Object> paramsToUpdate);
}
