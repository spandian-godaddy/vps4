package com.godaddy.vps4.hfs;

public class VmAction {

    public long vmActionId;

    public long vmId;

    public String state;

	@Override
	public String toString() {
		return "VmAction [vmActionId=" + vmActionId + ", vmId=" + vmId + ", state=" + state + "]";
	}

}
