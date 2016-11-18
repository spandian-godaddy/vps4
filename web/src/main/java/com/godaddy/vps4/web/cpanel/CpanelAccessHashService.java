package com.godaddy.vps4.web.cpanel;

import java.time.Instant;

public interface CpanelAccessHashService {

    String getAccessHash(long vmId, String publicIp, String fromIp, Instant timeoutAt);

    void invalidAccessHash(long vmId, String accessHash);
}
