package com.godaddy.hfs.sysadmin;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ChangePasswordRequestBody {
    public long serverId;
    public String username;
    public String password;
    public String controlPanel;

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
