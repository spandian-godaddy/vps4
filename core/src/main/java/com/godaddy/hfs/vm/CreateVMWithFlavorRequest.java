package com.godaddy.hfs.vm;


import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
    public String private_label_id;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
