package com.godaddy.vps4.vm;

public class VirtualMachine {

    public final long vmId;

    public final long projectId;

    public final VirtualMachineSpec spec;

    public final String name;

    public VirtualMachine(long vmId, long projectId, VirtualMachineSpec spec, String name) {
        this.vmId = vmId;
        this.projectId = projectId;
        this.spec = spec;
        this.name = name;
    }

    @Override
    public String toString() {
        return "VirtualMachine [vmId=" + vmId + ", projectId=" + projectId + ", spec="
                + spec + ", name=" + name + "]";
    }

}
