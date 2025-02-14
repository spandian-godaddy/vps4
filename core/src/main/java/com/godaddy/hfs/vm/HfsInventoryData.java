package com.godaddy.hfs.vm;

import com.fasterxml.jackson.annotation.JsonAlias;

public class HfsInventoryData {
    @JsonAlias("name") public String flavor;
    public int available;
    @JsonAlias("in_use") public int inUse;
    @JsonAlias("hfs_in_use") public int hfsInUse;
    public int reserved;
    public int retired;
}
