package com.godaddy.vps4.scheduledJob;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScheduledJob {

    public enum ScheduledJobType {
        BACKUPS_RETRY(1), ZOMBIE(2), REMOVE_SUPPORT_USER(3), BACKUPS_MANUAL(4), BACKUPS_AUTOMATIC(5);
        
        private int id;
        private static final Map<Integer, ScheduledJobType> byId = new HashMap<Integer, ScheduledJobType>();
        
        ScheduledJobType(int id) {
            this.id = id;
        }
        
        static {
            for (ScheduledJobType e : ScheduledJobType.values()) {
                if (byId.put(e.getId(), e) != null) {
                    throw new IllegalArgumentException("duplicate id: " + e.getId());
                }
            }
        }
        
        public int getId() {
            return id;
        }

        public static ScheduledJobType getById(int id) {
            return byId.get(id);
        }
        
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public ScheduledJob() {}
    
    public ScheduledJob(UUID id, UUID vmId, ScheduledJobType type, Instant created) {
        this.id = id;
        this.vmId = vmId;
        this.type = type;
        this.created = created;
    }
    
    public UUID id;
    public UUID vmId;
    public ScheduledJobType type;
    public Instant created;
}
