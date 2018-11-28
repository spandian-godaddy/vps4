package com.godaddy.hfs.vm;


public class Vm {

	public long vmId;
	
	public String status;
	public boolean running;
	public boolean useable;
	
	public String resource_id;
	public String resource;
	public String resource_uuid;
	public String resource_region;
	public String tag;
	
	public VmAddress address;
	public VmOSInfo osinfo;
	
	public String sgid;

    public long cpuCores;
    public long ramMiB;
    public long diskGiB;
    
    public String rawFlavor;

	// Dates in ISO format (or Null)
	public String created;
	public String prepared;
	public String disabled;
	
    @Override
    public String toString() {
        return "Vm [vmId=" + vmId + ", status=" + status + ", running=" + running + ", useable=" + useable
                + ", resource=" + resource + ", resource_id=" + resource_id + ", resource_uuid=" + resource_uuid
                + ", resource_region=" + resource_region + ", tag=" + tag + ", address=" + address + ", osinfo=" + osinfo
                + ", sgid=" + sgid + ", cpuCores=" + cpuCores + ", ramMiB=" + ramMiB + ", diskGiB=" + diskGiB
                + ", rawFlavor=" + rawFlavor + ", created=" + created + ", prepared=" + prepared + ", disabled=" + disabled + "]";
    }
}
