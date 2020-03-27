package com.godaddy.hfs.backupstorage;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/*
{
  "ftpServer": "ftpback-rbx4-99.ovh.net",
  "secret": "nvDs3QshdD",
  "ftpUser": "ns3160216.ip-151-106-35.eu"
}
*/

public class BackupStorageCreds {
    public String ftpServer;
    public String ftpUser;
    public String secret;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
