package com.godaddy.vps4.appmonitors;

import java.util.List;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public interface MonitorService {

    List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes, ActionStatus... status);
    List<HvBlockingSnapshotsData> getHvsBlockingSnapshots(long thresholdInHours);
    List<BackupJobAuditData> getVmsFilteredByNullBackupJob();
    MonitoringCheckpoint getMonitoringCheckpoint(ActionType actionType);
    MonitoringCheckpoint setMonitoringCheckpoint(ActionType actionType);
    void deleteMonitoringCheckpoint(ActionType actionType);
    List<MonitoringCheckpoint> getMonitoringCheckpoints();
}
