package com.godaddy.vps4.entitlement.models;

public class EntitlementProvisionRequest {
    public ManagementConsole managementConsole;
    public String commonName;
    public String provisioningTracker;
    public String result;
    public Service service;
    public Renewal renewal;


    public static class Service {
        public String start;
        public String end;
        public Service(String start, String end) {
            this.end = end;
            this.start = start;
        }
    }
    public static class Renewal {
        public String firstAutomated;
        public String lastPossible;
        public Renewal(String firstAutomated, String lastPossible) {
            this.firstAutomated = firstAutomated;
            this.lastPossible = lastPossible;
        }
    }
    public EntitlementProvisionRequest(ManagementConsole managementConsole, String commonName, String provisioningTracker,
                                       String result, Service service, Renewal renewal) {
        this.managementConsole = managementConsole;
        this.commonName = commonName;
        this.provisioningTracker = provisioningTracker;
        this.result = result;
        this.service = service;
        this.renewal = renewal;
    }
}
