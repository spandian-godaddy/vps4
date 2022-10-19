package com.godaddy.vps4.messaging;


import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.messaging.models.Message;

public interface Vps4MessagingService {

    Message getMessageById(String messageId);

    String sendSetupEmail(UUID customerId, String accountName, String ipAddress, String orionGuid, boolean isManaged);

    String sendFullyManagedEmail(UUID customerId, String controlPanel) throws MissingShopperIdException, IOException;

    String sendScheduledPatchingEmail(UUID customerId, String accountName, Instant startTime, long durationMinutes,
                                      boolean isManaged);

    String sendUnexpectedButScheduledMaintenanceEmail(UUID customerId, String accountName, Instant startTime,
                                                      long durationMinutes, boolean isManaged);

    String sendSystemDownFailoverEmail(UUID customerId, String accountName, boolean isManaged);

    String sendFailoverCompletedEmail(UUID customerId, String accountName, boolean isManaged);

    String sendUptimeOutageEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
                                 Instant alertStart, boolean isManaged);

    String sendServerUsageOutageEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
                                      String resourceName, String resourceUsage, Instant alertStart, boolean isManaged);

    String sendServicesDownEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
                                 String serviceName, Instant alertStart, boolean isManaged);

    String sendUptimeOutageResolvedEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
                                         Instant alertEnd, boolean isManaged);

    String sendUsageOutageResolvedEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
                                        String resourceName, Instant alertEnd, boolean isManaged);

    String sendServiceOutageResolvedEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
                                          String serviceName, Instant alertEnd, boolean isManaged);
}
