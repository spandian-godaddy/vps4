package com.godaddy.vps4.vm;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TroubleshootInfo {

    public StatusChecks status;

    public class StatusChecks {
        public boolean canPing;
        public boolean isPortOpen2224;
        public String hfsAgentStatus;
    }

    public TroubleshootInfo() {
        status = new StatusChecks();
    }

    public boolean isOk() {
        return status.canPing &&
               status.isPortOpen2224 &&
               status.hfsAgentStatus.equals("OK");
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
