package com.godaddy.vps4.hfs;

public class Vm {

	public long vmId;
	
	public String status;
	public boolean running;
	public boolean useable;
	
	public String resource_id;
	public String tag;
	
	public VmAddress address;
	public VmOSInfo osinfo;
	
	public String sgid;

    public long cpuCores;
    public long ramMiB;
    public long diskGiB;

	// Dates in ISO format (or Null)
	public String created;
	public String prepared;
	public String disabled;
	
	@Override
	public String toString() {
		return "Vm [vmId=" + vmId + ", status=" + status + ", running=" + running + ", useable=" + useable
				+ ", resource_id=" + resource_id + ", tag=" + tag + ", address=" + address + ", osinfo=" + osinfo
				+ ", sgid=" + sgid + ", cpuCores=" + cpuCores + ", ramMiB=" + ramMiB + ", diskGiB=" + diskGiB
				+ ", created=" + created + ", prepared=" + prepared + ", disabled=" + disabled + "]";
	}
	
}
