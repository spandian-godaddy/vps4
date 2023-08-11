package com.godaddy.vps4.plan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;

public class Plan {

    public int pfid;
    public String packageId;
    public int cpuCoreCount;
    public int memoryMib;
    public int diskGib;
    @JsonIgnore
    public boolean enabled;
    @JsonIgnore
    public OperatingSystem os;
    @JsonIgnore
    public int termMonths;
    public int tier;
    @JsonIgnore
    public ControlPanel controlPanel;
}
