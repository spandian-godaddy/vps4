package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;
import com.google.common.collect.Sets;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class SendVmOutageCreatedEmailTest {
    @Mock private MessagingService messagingService;
    @Mock private VmAlertService vmAlertService;
    @Mock private CommandContext context;

    VmOutageEmailRequest request = new VmOutageEmailRequest();
    VmOutage vmOutage = new VmOutage();
    List<VmMetricAlert> enabledAlerts = new ArrayList<>();
    String fakeMessageId = "fake-message-id";
    String fakeShopperId = "fake-shopper-id";
    String fakeAccountName = "fake-account-name";
    String fakeIpAddress = "127.0.0.1";
    UUID fakeOrionGuid = UUID.randomUUID();
    String fakeReason = "fake-reason";
    UUID fakeVmId = UUID.randomUUID();

    SendVmOutageCreatedEmail command;

    @Captor ArgumentCaptor<Function<CommandContext, String>> lambdaCaptor;

    @Before
    public void setUp() {
        Instant started = Instant.now();
        Instant ended = Instant.now();
        request.managed = true;
        request.shopperId = fakeShopperId;
        request.vmId = fakeVmId;
        request.accountName = fakeAccountName;
        request.ipAddress = fakeIpAddress;
        request.orionGuid = fakeOrionGuid;
        request.vmOutage = vmOutage;

        vmOutage.started = started;
        vmOutage.ended = ended;
        vmOutage.reason = fakeReason;
        vmOutage.domainMonitoringMetadata = new ArrayList<>();
        vmOutage.metrics = new HashSet<>();

        setupEnabledAlerts(null);
        command = new SendVmOutageCreatedEmail(messagingService, vmAlertService);
    }

    private void setupEnabledAlerts(VmMetric disabledMetric) {
        enabledAlerts = Arrays.stream(VmMetric.values()).map(metric -> {
            VmMetricAlert alert = new VmMetricAlert();
            alert.metric = metric;
            alert.status = VmMetricAlert.Status.ENABLED;
            return alert;
        }).filter(a -> a.metric != disabledMetric).collect(Collectors.toList());
        when(vmAlertService.getVmMetricAlertList(fakeVmId)).thenReturn(enabledAlerts);
    }

    @Test
    public void verifyUptimeOutageEmailIsSent() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.PING);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-PING"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        lambdaValue.apply(context);
        verify(messagingService, times(1))
                .sendUptimeOutageEmail(request.shopperId, request.accountName, request.ipAddress, request.orionGuid,
                                       request.vmOutage.started, request.managed);
    }

    @Test
    public void verifyAgentResourceOutageEmailIsSent() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.CPU);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-CPU"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        lambdaValue.apply(context);
        verify(messagingService, times(1))
                .sendServerUsageOutageEmail(request.shopperId, request.accountName, request.ipAddress,
                                            request.orionGuid, VmMetric.CPU.name(), "95%",
                                            request.vmOutage.started, request.managed);
    }

    @Test
    public void verifyNetworkServicesDownEmailIsSent() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.FTP);
        String fakeResourceName = VmMetric.FTP.name();
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq(fakeResourceName), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-Services"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSentHTTP() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.HTTP_DOMAIN);
        request.vmOutage.domainMonitoringMetadata =
                Collections.singletonList(new VmOutage.DomainMonitoringMetadata(
                        "domainfake.here",
                        Collections.singletonList("Unable to resolve host name domainfake.here"),
                        VmMetric.HTTP_DOMAIN
                ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTP_DOMAIN (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-HTTP_DOMAIN"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSentHTTPS() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.HTTPS_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL error: certificate verify failed"), VmMetric.HTTPS_DOMAIN
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTPS_DOMAIN (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-HTTPS_DOMAIN"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSentMultipleReasons() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.HTTPS_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL error: certificate verify failed",
                "SSL error: certificate verify failed"), VmMetric.HTTPS_DOMAIN
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTPS_DOMAIN (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-HTTPS_DOMAIN"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void doesNotSendOutageEmailForSSLWarning() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.HTTPS_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL certificate is expiring"), VmMetric.HTTPS_DOMAIN
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTPS (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(0))
                .execute(eq("SendVmOutageCreatedEmail-HTTPS_DOMAIN"), lambdaCaptor.capture(), eq(String.class));
    }

    @Test
    public void doesNotSendEmailIfAlertMetricIsDisabled() {
        setupEnabledAlerts(VmMetric.PING);

        request.vmOutage.metrics = Sets.newHashSet(VmMetric.PING);
        command.execute(context, request);

        verify(context, never())
                .execute(eq("SendVmOutageCreatedEmail-PING"), any(Function.class), eq(String.class));
    }

    @Test
    public void doesNotSendEmailIfHTTPSDomainMetricIsDisabled() {
        setupEnabledAlerts(VmMetric.HTTPS_DOMAIN);

        request.vmOutage.metrics = Sets.newHashSet(VmMetric.HTTPS_DOMAIN, VmMetric.HTTP_DOMAIN);
        request.vmOutage.domainMonitoringMetadata =
                Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                        "domainfake.here",
                        Collections.singletonList("Unable to resolve host name domainfake.here"),
                        VmMetric.HTTP_DOMAIN
                ), new VmOutage.DomainMonitoringMetadata(
                        "domainfake2.here",
                        Collections.singletonList("Unable to resolve host name domainfake2.here"),
                        VmMetric.HTTPS_DOMAIN));

        command.execute(context, request);

        verify(context, never())
                .execute(eq("SendVmOutageCreatedEmail-HTTPS_DOMAIN"), any(Function.class), eq(String.class));
        verify(context, never())
                .execute(eq("SendVmOutageCreatedEmail-HTTP_DOMAIN"), any(Function.class), eq(String.class));
    }

    @Test
    public void sendsEmailIfHTTPSDomainMetricNotDisabled() {
        setupEnabledAlerts(VmMetric.HTTP_DOMAIN);

        request.vmOutage.metrics = Sets.newHashSet(VmMetric.HTTPS_DOMAIN, VmMetric.HTTP_DOMAIN);
        request.vmOutage.domainMonitoringMetadata =
                Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                        "domainfake.here",
                        Collections.singletonList("Unable to resolve host name domainfake.here"),
                        VmMetric.HTTP_DOMAIN
                ), new VmOutage.DomainMonitoringMetadata(
                        "domainfake2.here",
                        Collections.singletonList("Unable to resolve host name domainfake2.here"),
                        VmMetric.HTTPS_DOMAIN));

        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-HTTPS_DOMAIN"), any(Function.class), eq(String.class));
        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-HTTP_DOMAIN"), any(Function.class), eq(String.class));

    }

    @Test
    public void passesCorrectValueToOutageEmail() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.DISK);
        request.vmOutage.reason = "vps4_disk_total_percent_used greater than 42% for more than 10 minutes";

        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-DISK"), lambdaCaptor.capture(), eq(String.class));

        lambdaCaptor.getValue().apply(context);
        verify(messagingService, times(1)).sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName),
                                                                      eq(fakeIpAddress), eq(fakeOrionGuid),
                                                                      eq(VmMetric.DISK.name()), eq("42%"),
                                                                      any(Instant.class), eq(true));
    }

    @Test
    public void passesDefaultValueToOutageEmail() {
        request.vmOutage.metrics = Sets.newHashSet(VmMetric.DISK);
        request.vmOutage.reason = "unexpected string";

        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-DISK"), lambdaCaptor.capture(), eq(String.class));

        lambdaCaptor.getValue().apply(context);
        verify(messagingService, times(1)).sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName),
                                                                      eq(fakeIpAddress), eq(fakeOrionGuid),
                                                                      eq(VmMetric.DISK.name()), eq("95%"),
                                                                      any(Instant.class), eq(true));
    }

    @Test
    public void handlesOutageWithMultipleMetrics() {
        request.vmOutage.metrics = new HashSet<>();
        request.vmOutage.metrics.add(VmMetric.HTTP_DOMAIN);
        request.vmOutage.metrics.add(VmMetric.IMAP);
        request.vmOutage.metrics.add(VmMetric.SSH);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("Unable to resolve host name domainfake.here"), VmMetric.HTTP_DOMAIN
        ));
        request.vmOutage.reason = "unexpected string";

        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-HTTP_DOMAIN"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
        verify(context, times(1))
                .execute(eq("SendVmOutageCreatedEmail-Services"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
    }
}