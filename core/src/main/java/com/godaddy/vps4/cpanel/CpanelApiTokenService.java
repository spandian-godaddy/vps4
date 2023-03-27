package com.godaddy.vps4.cpanel;

import java.time.Instant;

public interface CpanelApiTokenService {

    String getApiToken(long vmId, Instant timeoutAt);

    void invalidateApiToken(long vmId, String apiToken);
}
