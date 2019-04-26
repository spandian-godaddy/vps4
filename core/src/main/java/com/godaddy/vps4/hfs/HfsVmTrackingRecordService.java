package com.godaddy.vps4.hfs;

import java.util.List;
import java.util.UUID;

public interface HfsVmTrackingRecordService {

    HfsVmTrackingRecord get(long hfsVmId);

    HfsVmTrackingRecord create(long hfsVmId, UUID vmId, UUID orionGuid);
    
    void setCreated(long hfsVmId);

    void setCanceled(long hfsVmId);
    
    void setDestroyed(long hfsVmId);

    List<HfsVmTrackingRecord> getCanceled();

    List<HfsVmTrackingRecord> getUnused();

    List<HfsVmTrackingRecord> getRequested();
}
