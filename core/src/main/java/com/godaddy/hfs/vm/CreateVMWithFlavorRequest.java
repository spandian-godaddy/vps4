package com.godaddy.hfs.vm;

public class CreateVMWithFlavorRequest {

    public String sgid;
    public String hostname;
    public String image_name;
    public String rawFlavor;
    public String username;
    public String password;
    public String zone;
    public String image_id;
    public String ignore_whitelist;
    public String os;

    @Override
    public String toString() {
        return "CreateVMRequest [sgid=" + sgid + ", hostname=" + hostname + ", image_name=" + image_name
                + ", rawFlavor=" + rawFlavor + ", username=" + username + ", password=" + password + ", zone=" + zone
                + ", image_id=" + image_id + ", ignore_whitelist=" + ignore_whitelist + ", os=" + os + "]";
    }
}
