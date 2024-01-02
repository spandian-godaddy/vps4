package com.godaddy.vps4.web.vm;

import java.util.List;

import com.godaddy.hfs.vm.HfsInventoryData;

public class HfsInventoryDataWrapper {
    public List<HfsInventoryData> value;

    // Empty constructor required for Jackson
    public HfsInventoryDataWrapper() {}

    public HfsInventoryDataWrapper(List<HfsInventoryData> value) {
        this.value = value;
    }
}
