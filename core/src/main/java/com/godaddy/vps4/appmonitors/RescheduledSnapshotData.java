package com.godaddy.vps4.appmonitors;

public class RescheduledSnapshotData {
    public int rescheduledSnapshots;
    public int affectedVms;

    public RescheduledSnapshotData() {}

    public RescheduledSnapshotData(int rescheduledSnapshots, int affectedVms) {
        this.rescheduledSnapshots = rescheduledSnapshots;
        this.affectedVms = affectedVms;
    }
}
