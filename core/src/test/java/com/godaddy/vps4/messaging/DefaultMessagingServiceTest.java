package com.godaddy.vps4.messaging;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.messaging.models.MessagingResponse;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.messaging.models.Substitution;
import com.godaddy.vps4.messaging.models.TemplateType;
import com.godaddy.vps4.messaging.models.Transformation;

@RunWith(MockitoJUnitRunner.class)
public class DefaultMessagingServiceTest {
    @Captor private ArgumentCaptor<ShopperMessage> captor;
    @Mock private MessagingApiService messagingApiService;

    private MessagingResponse messagingResponse;
    private DefaultMessagingService messagingService;

    private final String shopperId = UUID.randomUUID().toString();
    private final String accountName = UUID.randomUUID().toString();
    private final String ipAddress = UUID.randomUUID().toString();
    private final UUID orionGuid = UUID.randomUUID();
    private final String orionId = orionGuid.toString();
    private final Boolean isManaged = false;
    private final long durationMinutes = 60;
    private final Instant startTime = Instant.parse("2018-11-30T18:35:24.00Z");
    private final Instant endTime = startTime.plus(durationMinutes, ChronoUnit.MINUTES);
    private final String resourceName = "CPU";
    private final String resourceUsage = "95%";

    @Before
    public void setUp() {
        messagingResponse = new MessagingResponse();
        messagingResponse.messageId = UUID.randomUUID().toString();

        when(messagingApiService.sendMessage(eq(shopperId), any(ShopperMessage.class)))
                .thenReturn(messagingResponse);

        messagingService = new DefaultMessagingService(messagingApiService);
    }

    private void testEmail(Callable<String> method,
                           TemplateType type,
                           EnumMap<Substitution, String> substitutions,
                           EnumMap<Transformation, String> transformations) throws Exception {
        String messageId = method.call();
        Assert.assertEquals(messagingResponse.messageId, messageId);
        verify(messagingApiService).sendMessage(eq(shopperId), captor.capture());
        ShopperMessage message = captor.getValue();
        Assert.assertEquals(type, message.templateTypeKey);
        if (substitutions == null) {
            Assert.assertNull(message.substitutionValues);
        } else {
            Assert.assertEquals(substitutions.size(), message.substitutionValues.size());
            substitutions.forEach((key, value) -> Assert.assertEquals(value, message.substitutionValues.get(key)));
        }
        if (transformations == null) {
            Assert.assertNull(message.transformationData);
        } else {
            Assert.assertEquals(transformations.size(), message.transformationData.size());
            transformations.forEach((key, value) -> Assert.assertEquals(value, message.transformationData.get(key)));
        }
    }

    private void testOutageEmail(Callable<String> method,
                                 TemplateType type,
                                 EnumMap<Substitution, String> substitutions,
                                 EnumMap<Transformation, String> transformations) throws Exception {
        substitutions.putAll(new EnumMap<Substitution, String>(Substitution.class) {{
            put(Substitution.ACCOUNTNAME, accountName);
            put(Substitution.SERVERNAME, accountName);
            put(Substitution.IPADDRESS, ipAddress);
            put(Substitution.ORION_ID, orionId);
            put(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        }});
        testEmail(method, type, substitutions, transformations);
    }

    @Test
    public void testSendSetupEmail() throws Exception {
        testEmail(() -> messagingService.sendSetupEmail(shopperId, accountName, ipAddress, orionId, isManaged),
                  TemplateType.VirtualPrivateHostingProvisioned4,
                  new EnumMap<Substitution, String>(Substitution.class) {{
                      put(Substitution.ACCOUNTNAME, accountName);
                      put(Substitution.IPADDRESS, ipAddress);
                      put(Substitution.ORION_ID, orionId);
                      put(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
                  }}, null);
    }

    @Test
    public void testSendFullyManagedEmail() throws Exception {
        testEmail(() -> messagingService.sendFullyManagedEmail(shopperId, "cpanel"),
                  TemplateType.VPSWelcomeCpanel, null, null);
    }

    @Test
    public void testSendScheduledPatchingEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendScheduledPatchingEmail(shopperId, accountName, startTime, durationMinutes, isManaged);
        testEmail(method,
                  TemplateType.VPS4ScheduledPatchingV2,
                  new EnumMap<Substitution, String>(Substitution.class) {{
                      put(Substitution.ACCOUNTNAME, accountName);
                      put(Substitution.START_DATE_TIME, "2018-11-30 18:35:24");
                      put(Substitution.END_DATE_TIME, "2018-11-30 19:35:24");
                      put(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
                  }}, null);
    }

    @Test
    public void testSendUnexpectedButScheduledMaintenanceEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName,
                                                            startTime, durationMinutes, isManaged);
        testEmail(method,
                  TemplateType.VPS4UnexpectedbutScheduledMaintenanceV2,
                  new EnumMap<Substitution, String>(Substitution.class) {{
                      put(Substitution.ACCOUNTNAME, accountName);
                      put(Substitution.START_DATE_TIME, "2018-11-30 18:35:24");
                      put(Substitution.END_DATE_TIME, "2018-11-30 19:35:24");
                      put(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
                  }}, null);
    }

    @Test
    public void testSendSystemDownFailoverEmail() throws Exception {
        testEmail(() -> messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isManaged),
                  TemplateType.VPS4SystemDownFailoverV2,
                  new EnumMap<Substitution, String>(Substitution.class) {{
                      put(Substitution.ACCOUNTNAME, accountName);
                      put(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
                  }}, null);
    }

    @Test
    public void testSendFailoverCompletedEmail() throws Exception {
        testEmail(() -> messagingService.sendFailoverCompletedEmail(shopperId, accountName, isManaged),
                  TemplateType.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted,
                  new EnumMap<Substitution, String>(Substitution.class) {{
                      put(Substitution.ACCOUNTNAME, accountName);
                      put(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
                  }}, null);
    }

    @Test
    public void testSendUptimeOutageEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendUptimeOutageEmail(shopperId, accountName, ipAddress, orionGuid, startTime, isManaged);
        testOutageEmail(method, TemplateType.NewFinalSelfManagedUptime,
                        new EnumMap<Substitution, String>(Substitution.class) {{
                        }},
                        new EnumMap<Transformation, String>(Transformation.class) {{
                            put(Transformation.ALERTSTARTTIME, startTime.toString());
                        }});
    }

    @Test
    public void testSendServerUsageOutageEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendServerUsageOutageEmail(shopperId, accountName, ipAddress, orionGuid, "CPU", "95%", startTime, isManaged);
        testOutageEmail(method, TemplateType.NewFinalSelfManagedServerUsage,
                        new EnumMap<Substitution, String>(Substitution.class) {{
                            put(Substitution.SERVICENAME, resourceName);
                            put(Substitution.RESOURCENAME, resourceName);
                            put(Substitution.RESOURCEUSAGE, resourceUsage);
                        }},
                        new EnumMap<Transformation, String>(Transformation.class) {{
                            put(Transformation.ALERTSTARTTIME, startTime.toString());
                        }});
    }

    @Test
    public void testSendServicesDownEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendServicesDownEmail(shopperId, accountName, ipAddress, orionGuid, "CPU", startTime, isManaged);
        testOutageEmail(method, TemplateType.NewFinalSelfManagedServicesDown,
                        new EnumMap<Substitution, String>(Substitution.class) {{
                            put(Substitution.SERVICENAME, resourceName);
                            put(Substitution.RESOURCENAME, resourceName);
                        }},
                        new EnumMap<Transformation, String>(Transformation.class) {{
                            put(Transformation.ALERTSTARTTIME, startTime.toString());
                        }});
    }

    @Test
    public void testSendUptimeOutageResolvedEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendUptimeOutageResolvedEmail(shopperId, accountName, ipAddress, orionGuid, endTime, isManaged);
        testOutageEmail(method, TemplateType.VPS_DED_4_Issue_Resolved_Uptime,
                        new EnumMap<>(Substitution.class),
                        new EnumMap<Transformation, String>(Transformation.class) {{
                            put(Transformation.ALERTENDTIME, startTime.plus(durationMinutes, ChronoUnit.MINUTES).toString());
                        }});
    }

    @Test
    public void testSendUsageOutageResolvedEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendUsageOutageResolvedEmail(shopperId, accountName, ipAddress, orionGuid, resourceName, endTime, isManaged);
        testOutageEmail(method, TemplateType.VPS_DED_4_Issue_Resolved_Resources,
                        new EnumMap<Substitution, String>(Substitution.class) {{
                            put(Substitution.SERVICENAME, resourceName);
                            put(Substitution.RESOURCENAME, resourceName);
                        }},
                        new EnumMap<Transformation, String>(Transformation.class) {{
                            put(Transformation.ALERTENDTIME, startTime.plus(durationMinutes, ChronoUnit.MINUTES).toString());
                        }});
    }

    @Test
    public void testSendServiceOutageResolvedEmail() throws Exception {
        Callable<String> method = () -> messagingService
                .sendServiceOutageResolvedEmail(shopperId, accountName, ipAddress, orionGuid, resourceName, endTime, isManaged);
        testOutageEmail(method, TemplateType.VPS_DED_4_Issue_Resolved_Services,
                        new EnumMap<Substitution, String>(Substitution.class) {{
                            put(Substitution.SERVICENAME, resourceName);
                            put(Substitution.RESOURCENAME, resourceName);
                        }},
                        new EnumMap<Transformation, String>(Transformation.class) {{
                            put(Transformation.ALERTENDTIME, startTime.plus(durationMinutes, ChronoUnit.MINUTES).toString());
                        }});
    }
}
