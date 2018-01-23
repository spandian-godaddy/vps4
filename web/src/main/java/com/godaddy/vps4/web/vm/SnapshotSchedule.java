package com.godaddy.vps4.web.vm;
import java.time.Instant;

public class SnapshotSchedule {

    public Instant nextAt;
    public int copiesToRetain;
    public int repeatIntervalInDays;

    public SnapshotSchedule(){}
    public SnapshotSchedule(Instant nextAt, int copiesToRetain, int repeatIntervalInDays){
        this.nextAt = nextAt;
        this.copiesToRetain = copiesToRetain;
        this.repeatIntervalInDays = repeatIntervalInDays;
    }
}
