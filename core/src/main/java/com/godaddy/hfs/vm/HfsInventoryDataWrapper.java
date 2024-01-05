package com.godaddy.hfs.vm;

import java.util.List;

public class HfsInventoryDataWrapper {
    public List<HfsInventoryData> value;

    // Empty constructor required for Jackson
    public HfsInventoryDataWrapper() {}

    public HfsInventoryDataWrapper(List<HfsInventoryData> value) {
        this.value = value;
    }
}
