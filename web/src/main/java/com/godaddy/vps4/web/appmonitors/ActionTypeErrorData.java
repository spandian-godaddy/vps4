package com.godaddy.vps4.web.appmonitors;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionType;

import java.util.List;

public class ActionTypeErrorData {
    public ActionType actionType;
    public double failurePercentage;
    public List<Action> failedActions;

    public ActionTypeErrorData(ActionType type, double failurePercentage, List<Action> failedActions) {
        actionType = type;
        this.failurePercentage = failurePercentage;
        this.failedActions = failedActions;
    }
}
