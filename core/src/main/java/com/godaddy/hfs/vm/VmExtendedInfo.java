package com.godaddy.hfs.vm;

public class VmExtendedInfo {
    public String provider;
    public String resource;
    public Extended extended;

    @Override
    public String toString() {
        return "VmExtendedInfo [provider=" + provider + ", resource=" + resource + ", extended=" + extended + "]";
    }
}