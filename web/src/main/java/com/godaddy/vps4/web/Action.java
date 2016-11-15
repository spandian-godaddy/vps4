package com.godaddy.vps4.web;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public abstract class Action {

    public Action() {
        status = ActionStatus.IN_PROGRESS;
    }

    public long actionId;
    public volatile ActionStatus status;
    public ActionType type;
    public volatile String message;
}