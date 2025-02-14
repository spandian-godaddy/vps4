package com.godaddy.vps4.web.vm;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.godaddy.hfs.vm.Vm;

public class VirtualMachineDetails {

    public Long vmId;
    public String status;
    public boolean running;
    public boolean useable;
    public String resourceId;

    public VirtualMachineDetails (Vm hfsVm) {
        if (hfsVm == null) {
            vmId = null;
            status = "REQUESTING";
            running = false;
            useable = false;
            resourceId = null;
        }
        else {
            vmId = hfsVm.vmId;
            status = hfsVm.status;
            running = hfsVm.running;
            useable = hfsVm.useable;
            resourceId = hfsVm.resourceId;
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
