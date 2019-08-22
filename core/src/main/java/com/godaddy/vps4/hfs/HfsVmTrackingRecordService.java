package com.godaddy.vps4.hfs;

import java.util.List;
import java.util.UUID;

public interface HfsVmTrackingRecordService {

    static enum Status {
        UNUSED, CANCELED, REQUESTED
    }

    static class ListFilters {
        public UUID vmId;
        public long hfsVmId;
        public String sgid;
        public Status byStatus;
    }

    HfsVmTrackingRecord get(long hfsVmId);

    HfsVmTrackingRecord create(long hfsVmId, UUID vmId, UUID orionGuid);

    void setCreated(long hfsVmId, long actionId);

    void setCanceled(long hfsVmId, long actionId);

    void setDestroyed(long hfsVmId, long actionId);

    List<HfsVmTrackingRecord> getTrackingRecords(ListFilters listFilters);
}
