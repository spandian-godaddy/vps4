package com.godaddy.hfs.vm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Vm {

	public long vmId;
	
	public String status;
	public boolean running;
	public boolean useable;
	
    @JsonProperty("resource_id")
	public String resourceId;
	public String resource;
    @JsonProperty("resource_uuid")
	public String resourceUuid;
    @JsonProperty("resource_region")
	public String resourceRegion;
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
                + ", resource=" + resource + ", resource_id=" + resourceId + ", resource_uuid=" + resourceUuid
                + ", resource_region=" + resourceRegion + ", tag=" + tag + ", address=" + address + ", osinfo=" + osinfo
                + ", sgid=" + sgid + ", cpuCores=" + cpuCores + ", ramMiB=" + ramMiB + ", diskGiB=" + diskGiB
                + ", rawFlavor=" + rawFlavor + ", created=" + created + ", prepared=" + prepared + ", disabled=" + disabled + "]";
    }
}
