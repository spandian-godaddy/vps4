package com.godaddy.vps4.hfs;

public class ProvisionVMRequest {

    public String sgid;

    public String hostname;

    public String os;

    public String image_name;

    public int cpuCores;

    public int diskGiB;

    public int ramMiB;

    public String username;

    public String password;

    @Override
    public String toString() {
        return "CreateVMRequest [sgid=" + sgid + ", hostname=" + hostname + ", os=" + os + ", image_name=" + image_name + ", cpuCores="
                + cpuCores + ", diskGiB=" + diskGiB + ", ramMiB=" + ramMiB + ", username=" + username + ", password=" + password + "]";
    }

}
