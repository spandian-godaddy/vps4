package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.web.Action;
import com.godaddy.vps4.vm.ActionType;

/**
 * Created by abhoite on 11/9/16.
 */
public class ManageVmAction extends Action {

    public ManageVmAction(long vmId, ActionType actionType) {
        this.vmId = vmId;
        this.actionType = actionType;
    }

    public ManageVmAction(long vmId, ActionType actionType, String message) {
        this.vmId = vmId;
        this.actionType = actionType;
        this.message = message;
    }

    private long vmId;
    private ActionType actionType;

    public long getVmId() {
        return vmId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getMessage() {
        return this.message;
    }

    public long getActionId() { return this.actionId; }

    public void setActionId(long id) {
        this.actionId = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
