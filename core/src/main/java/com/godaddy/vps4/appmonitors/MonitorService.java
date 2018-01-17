package com.godaddy.vps4.appmonitors;

import java.util.List;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public interface MonitorService {

    List<VmActionData> getVmsByActions(ActionType type, ActionStatus status, long thresholdInMinutes);
    List<SnapshotActionData> getVmsBySnapshotActions(ActionType type, ActionStatus status, long thresholdInMinutes);
}
