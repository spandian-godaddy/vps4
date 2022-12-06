package com.godaddy.vps4.appmonitors;

import com.godaddy.vps4.vm.ActionType;

import java.time.Instant;

public class ActionCheckpoint {
    public ActionType actionType;
    public Instant checkpoint;

    public int getActionTypeId() {
        return actionType.getActionTypeId();
    }
}
