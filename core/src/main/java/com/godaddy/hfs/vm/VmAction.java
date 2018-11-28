package com.godaddy.hfs.vm;


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
    

    @Override
    public String toString() {
        return String.format("VmAction [vmActionId=%d, vmId=%d, actionType=%s, state=%s, tickNum=%d, numTicks=%d, tickInfo=%s, createdAt=%s, completedAt=%s, message=%s",
        		vmActionId, vmId, actionType, state, tickNum, numTicks, tickInfo, createdAt, completedAt, message);
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
