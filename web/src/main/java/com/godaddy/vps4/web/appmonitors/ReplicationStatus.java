package com.godaddy.vps4.web.appmonitors;

import java.util.ArrayList;

public class ReplicationStatus {
    public String masterServer;
    public ArrayList<StandbyServer> standbyServers;

    public ReplicationStatus(String masterServer) {
        this.masterServer = masterServer;
        this.standbyServers = new ArrayList<>();
    }

    public static class StandbyServer {
        public String name;
        public double lagInMb;

        public StandbyServer(String name, double lagInMb) {
            this.name = name;
            this.lagInMb = lagInMb;
        }
    }
}
