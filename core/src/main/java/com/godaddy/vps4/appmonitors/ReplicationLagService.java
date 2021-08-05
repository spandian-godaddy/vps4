package com.godaddy.vps4.appmonitors;

public interface ReplicationLagService {

    boolean isMasterServer(String hostname);

    String getCurrentLocation(String hostname);

    String getLastReceiveLocation(String hostname);

    long comparePgLsns(String hostname, String lsn1, String lsn2);

}
