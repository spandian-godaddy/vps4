package com.godaddy.vps4.messaging;


import java.io.IOException;
import java.time.Instant;

import com.godaddy.vps4.messaging.models.Message;

public interface Vps4MessagingService {

    Message getMessageById(String messageId)
            throws IOException;

    String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionGuid,
                          boolean isFullyManaged)
            throws MissingShopperIdException, IOException;

    String sendFullyManagedEmail(String shopperId, String controlPanel) throws MissingShopperIdException, IOException;

    String sendScheduledPatchingEmail(String shopperId, String accountName, Instant startTime, long durationMinutes,
                                      boolean isFullyManaged)
            throws MissingShopperIdException, IOException;

    String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String accountName, Instant startTime,
                                                      long durationMinutes, boolean isFullyManaged)
            throws MissingShopperIdException, IOException;

    String sendSystemDownFailoverEmail(String shopperId, String accountName, boolean isFullyManaged)
            throws MissingShopperIdException, IOException;

    String sendFailoverCompletedEmail(String shopperId, String accountName, boolean isFullyManaged)
            throws MissingShopperIdException, IOException;
}
