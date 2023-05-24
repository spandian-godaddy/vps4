package com.godaddy.vps4.messaging;


import java.time.Instant;
import java.util.UUID;

public interface MessagingService {

    String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionGuid, boolean isManaged);

    String sendFullyManagedEmail(String shopperId, String controlPanel);

    String sendScheduledPatchingEmail(String shopperId, String accountName, Instant startTime, long durationMinutes,
                                      boolean isManaged);

    String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String accountName, Instant startTime,
                                                      long durationMinutes, boolean isManaged);

    String sendSystemDownFailoverEmail(String shopperId, String accountName, boolean isManaged);

    String sendFailoverCompletedEmail(String shopperId, String accountName, boolean isManaged);

    String sendUptimeOutageEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                 Instant alertStart, boolean isManaged);

    String sendServerUsageOutageEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                      String resourceName, String resourceUsage, Instant alertStart, boolean isManaged);

    String sendServicesDownEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                 String services, Instant alertStart, boolean isManaged);

    String sendUptimeOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                         Instant alertEnd, boolean isManaged);

    String sendUsageOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                        String resourceName, Instant alertEnd, boolean isManaged);

    String sendServiceOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                          String serviceName, Instant alertEnd, boolean isManaged);
}
