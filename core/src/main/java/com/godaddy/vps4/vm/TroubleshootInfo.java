package com.godaddy.vps4.vm;

public class TroubleshootInfo {

    public StatusChecks status;

    public class StatusChecks {
        public boolean canPing;
        public boolean isPortOpen2223;
        public boolean isPortOpen2224;
    }

    public TroubleshootInfo() {
        status = new StatusChecks();
    }

    public boolean isOk() {
        return status.canPing &&
               status.isPortOpen2223 &&
               status.isPortOpen2224;
    }

    @Override
    public String toString() {
        return "[canPing: " + status.canPing
                + ", isPortOpen2223: " + status.isPortOpen2223
                + ", isPortOpen2223: " + status.isPortOpen2223 + "]";
    }
}
