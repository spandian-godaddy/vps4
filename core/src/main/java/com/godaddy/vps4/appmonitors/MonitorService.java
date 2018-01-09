package com.godaddy.vps4.appmonitors;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public interface MonitorService {

    List<UUID> getVmsByActions(ActionType type, ActionStatus status, long thresholdInMinutes);
}
