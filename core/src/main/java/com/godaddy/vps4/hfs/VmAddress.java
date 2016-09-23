package com.godaddy.vps4.hfs;

public class VmAddress {
   public String ip_address;
   public String netmask;
   public String gateway;
   public int vlan;
   
	@Override
	public String toString() {
		return "VmAddress [ip_address=" + ip_address + ", netmask=" + netmask + ", gateway=" + gateway + ", vlan=" + vlan
				+ "]";
	}
   
}
