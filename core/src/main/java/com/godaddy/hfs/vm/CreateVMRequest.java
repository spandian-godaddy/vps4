package com.godaddy.hfs.vm;

public class CreateVMRequest {

    public String sgid;
    public String hostname;
    public String image_name;
    public int cpuCores;
    public int diskGiB;
    public int ramMiB;
    public String username;
    public String password;
    public String image_id;
    public String ignore_whitelist;
    public String os;


    @Override
    public String toString() {
        return "CreateVMRequest [sgid=" + sgid + ", hostname=" + hostname + ", image_name=" + image_name
                + ", cpuCores=" + cpuCores + ", diskGiB=" + diskGiB + ", ramMiB=" + ramMiB + ", username=" + username
                + ", password=" + password + ", image_id=" + image_id + ", ignore_whitelist=" + ignore_whitelist
                + ", os=" + os + "]";
    }
}
