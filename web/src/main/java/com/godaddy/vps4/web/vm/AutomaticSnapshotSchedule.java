package com.godaddy.vps4.web.vm;
import java.time.Instant;

public class AutomaticSnapshotSchedule {

    public Instant nextAt;
    public int copiesToRetain;
    public int repeatIntervalInDays;
    public boolean isPaused;

    public AutomaticSnapshotSchedule(){}
    public AutomaticSnapshotSchedule(Instant nextAt, int copiesToRetain, int repeatIntervalInDays, boolean isPaused){
        this.nextAt = nextAt;
        this.copiesToRetain = copiesToRetain;
        this.repeatIntervalInDays = repeatIntervalInDays;
        this.isPaused = isPaused;
    }
}
