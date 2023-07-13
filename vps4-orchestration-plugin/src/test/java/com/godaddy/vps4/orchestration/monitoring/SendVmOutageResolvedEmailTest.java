package com.godaddy.vps4.orchestration.monitoring;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class SendVmOutageResolvedEmailTest {
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

    SendVmOutageResolvedEmail command;

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

        setupEnabledAlerts();

        command = new SendVmOutageResolvedEmail(messagingService, vmAlertService);
    }

    private void setupEnabledAlerts() {
        enabledAlerts = Arrays.stream(VmMetric.values()).map(metric -> {
            VmMetricAlert alert = new VmMetricAlert();
            alert.metric = metric;
            alert.status = VmMetricAlert.Status.ENABLED;
            return alert;
        }).collect(Collectors.toList());
        when(vmAlertService.getVmMetricAlertList(fakeVmId)).thenReturn(enabledAlerts);
    }

    @Test
    public void verifyUptimeOutageResolvedEmailIsSent() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.PING);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-PING"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        lambdaValue.apply(context);
        verify(messagingService, times(1))
                .sendUptimeOutageResolvedEmail(request.shopperId, request.accountName, request.ipAddress,
                                               request.orionGuid, request.vmOutage.started, request.managed);
    }

    @Test
    public void verifyAgentResourceOutageResolvedEmailIsSent() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.CPU);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-CPU"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        lambdaValue.apply(context);
        verify(messagingService, times(1))
                .sendUsageOutageResolvedEmail(request.shopperId, request.accountName, request.ipAddress,
                                              request.orionGuid, VmMetric.CPU.name(),
                                              request.vmOutage.ended, request.managed);
    }

    @Test
    public void verifyNetworkServicesRestoredEmailIsSent() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.FTP);
        String fakeResourceName = VmMetric.FTP.name();
        when(messagingService
                     .sendServiceOutageResolvedEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                                            eq(fakeResourceName), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-Services"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesRestoredEmailIsSentHTTP() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.HTTP_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("Unable to resolve host name domainfake.here"), VmMetric.HTTP_DOMAIN
        ));

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(messagingService
                     .sendServiceOutageResolvedEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress),
                                                     eq(fakeOrionGuid), eq("HTTP_DOMAIN (domainfake.here)"), any(Instant.class),
                                                     eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-HTTP_DOMAIN"), lambdaCaptor.capture(),
                         eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesRestoredEmailIsSentHTTPS() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL error: certificate verify failed"), VmMetric.HTTPS_DOMAIN
        ));

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(messagingService
                .sendServiceOutageResolvedEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq("HTTPS_DOMAIN (domainfake.here)"), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-HTTPS_DOMAIN"), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void sendsOutageForResolvedEmailMultipleReasons() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here",
                Arrays.asList("SSL certificate is expiring", "SSL error: certificate verify failed"),
                VmMetric.HTTPS_DOMAIN
        ));

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(messagingService
                .sendServiceOutageResolvedEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq("HTTPS_DOMAIN (domainfake.here)"), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-HTTPS_DOMAIN"), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void doesNotSendOutageResolvedEmailForSSLWarning() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS_DOMAIN);
        request.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL certificate is expiring"), VmMetric.HTTPS_DOMAIN
        ));

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(messagingService
                .sendServiceOutageResolvedEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq("HTTPS (domainfake.here)"), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, request);

        verify(context, times(0))
                .execute(eq("SendVmOutageResolvedEmail-HTTPS"), lambdaCaptor.capture(),
                        eq(String.class));
    }


    @Test
    public void doesNotSendEmailIfAlertMetricIsDisabled() {
        request.vmOutage.metrics = Collections.singleton(VmMetric.PING);
        command.execute(context, request);

        verify(context, never())
                .execute(eq("SendVmOutageResolvedEmail-" + fakeShopperId), any(Function.class),
                         eq(String.class));
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
                .execute(eq("SendVmOutageResolvedEmail-HTTP_DOMAIN"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-Services"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
    }
}