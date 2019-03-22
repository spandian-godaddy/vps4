package com.godaddy.hfs.vm;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RebuildDedicatedRequest {
    public String hostname;
    public String image_id;
    public String image_name;
    public String username;
    public String password;
    public String os;
    public String ignore_whitelist;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
