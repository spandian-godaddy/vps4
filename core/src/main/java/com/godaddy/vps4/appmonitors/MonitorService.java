package com.godaddy.vps4.appmonitors;

import java.util.List;

import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public interface MonitorService {
    List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes, ActionStatus... status);
    List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes,
                                                     SnapshotType type,
                                                     ActionStatus... status);
    List<HvBlockingSnapshotsData> getHvsBlockingSnapshots(long thresholdInHours);
    ActionCheckpoint getActionCheckpoint(ActionType actionType);
    ActionCheckpoint setActionCheckpoint(ActionType actionType);
    void deleteActionCheckpoint(ActionType actionType);
    List<ActionCheckpoint> getActionCheckpoints();
    Checkpoint getCheckpoint(Checkpoint.Name name);
    Checkpoint setCheckpoint(Checkpoint.Name name);
    void deleteCheckpoint(Checkpoint.Name name);
    List<Checkpoint> getCheckpoints();
}
