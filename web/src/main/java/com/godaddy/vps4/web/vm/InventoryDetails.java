package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.HfsInventoryData;

public class InventoryDetails extends HfsInventoryData {
    public int tier;
    public int cpus;
    public int ram;
    public int diskSize;
    public String diskType;
    public int vps4Active;
    public int vps4Zombie;

    // Empty constructor required for Jackson
    public InventoryDetails() {}

    public InventoryDetails(HfsInventoryData source) {
        this.flavor = source.flavor;
        this.available = source.available;
        this.inUse = source.inUse;
        this.hfsInUse = source.hfsInUse;
        this.reserved = source.reserved;
        this.retired = source.retired;
    }
}
