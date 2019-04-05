package com.godaddy.vps4.cpanel;

import java.time.Instant;

public interface CpanelAccessHashService {

    String getAccessHash(long vmId, String publicIp, Instant timeoutAt);

    void invalidAccessHash(long vmId, String accessHash);
}
