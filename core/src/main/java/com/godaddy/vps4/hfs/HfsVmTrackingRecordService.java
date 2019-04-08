package com.godaddy.vps4.hfs;

import java.util.UUID;

public interface HfsVmTrackingRecordService {

    HfsVmTrackingRecord getHfsVm(long hfsVmId);
    HfsVmTrackingRecord createHfsVm(long hfsVmId, UUID vmId, UUID orionGuid);
    void setHfsVmCreated(long hfsVmId);
    void setHfsVmCanceled(long hfsVmId);
    void setHfsVmDestroyed(long hfsVmId);
}
