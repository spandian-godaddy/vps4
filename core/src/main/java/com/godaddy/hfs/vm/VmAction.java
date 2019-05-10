package com.godaddy.hfs.vm;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmAction {

    public long vmActionId;
    public long vmId;
    public Status state;
    public int tickNum;
    public int numTicks;
    public String tickInfo;
    public String actionType;
    public String completedAt;
    public String createdAt;
    public String message;
    public String resultset;
    

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
    public enum Status {
    	NEW(1),
    	REQUESTED(2),
    	IN_PROGRESS(3),
        COMPLETE(4),
        ERROR(98);

        public final int id;

        private Status(int id) {
            this.id = id;
        }
    }

}
