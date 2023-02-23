package com.godaddy.vps4.orchestration.monitoring;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;

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
public class SendVmOutageEmailTest {
    @Mock private MessagingService messagingService;
    @Mock private VmAlertService vmAlertService;
    @Mock private CommandContext context;

    VmOutageEmailRequest vmOutageEmailRequest = new VmOutageEmailRequest();
    VmOutage vmOutage = new VmOutage();
    VmMetricAlert vmMetricAlert = new VmMetricAlert();
    String fakeMessageId = "fake-message-id";
    String fakeShopperId = "fake-shopper-id";
    String fakeAccountName = "fake-account-name";
    String fakeIpAddress = "127.0.0.1";
    UUID fakeOrionGuid = UUID.randomUUID();
    String fakeReason = "fake-reason";
    UUID fakeVmId = UUID.randomUUID();

    SendVmOutageEmail command;

    @Captor
    ArgumentCaptor<Function<CommandContext, String>> lambdaCaptor;

    @Before
    public void setUp() {
        Instant started = Instant.now();
        Instant ended = Instant.now();
        vmOutageEmailRequest.managed = true;
        vmOutageEmailRequest.shopperId = fakeShopperId;
        vmOutageEmailRequest.vmId = fakeVmId;
        vmOutageEmailRequest.accountName = fakeAccountName;
        vmOutageEmailRequest.ipAddress = fakeIpAddress;
        vmOutageEmailRequest.orionGuid = fakeOrionGuid;
        vmOutageEmailRequest.vmOutage = vmOutage;

        vmOutage.started = started;
        vmOutage.ended = ended;
        vmOutage.reason = fakeReason;
        vmOutage.domainMonitoringMetadata = new ArrayList<>();

        setupMocks();
        command = new SendVmOutageEmail(messagingService, vmAlertService);
    }

    private void setupMocks() {
        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);
    }

    @Test
    public void verifyUptimeOutageEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.PING);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(messagingService
                .sendUptimeOutageEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-PING"), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServerUsageOutageEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.CPU);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        String fakeResourceName = VmMetric.CPU.name();
        when(messagingService.sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress),
                                                         eq(fakeOrionGuid), eq(fakeResourceName), anyString(), any(Instant.class), eq(true)))
                .thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-CPU"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.FTP);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        String fakeResourceName = VmMetric.FTP.name();
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq(fakeResourceName), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-FTP"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSentHTTP() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTP);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("Unable to resolve host name domainfake.here"), VmMetric.HTTP
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTP (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-HTTP"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSentHTTPS() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL error: certificate verify failed"), VmMetric.HTTPS
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTPS (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-HTTPS"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSentMultipleReasons() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL error: certificate verify failed",
                "SSL error: certificate verify failed"), VmMetric.HTTPS
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTPS (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-HTTPS"), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void doesNotSendOutageEmailForSSLWarning() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL certificate is expiring"), VmMetric.HTTPS
        ));
        when(messagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq("HTTPS (domainfake.here)"), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(0))
                .execute(eq("SendVmOutageEmail-HTTPS"), lambdaCaptor.capture(), eq(String.class));
    }

    @Test
    public void doesNotSendEmailIfAlertMetricIsDisabled() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.PING);
        vmMetricAlert.status = VmMetricAlert.Status.DISABLED;
        command.execute(context, vmOutageEmailRequest);

        verify(context, never())
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), any(Function.class), eq(String.class));
    }

    @Test
    public void passesCorrectValueToOutageEmail() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.DISK);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.reason = "vps4_disk_total_percent_used greater than 42% for more than 10 minutes";

        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-DISK"), lambdaCaptor.capture(), eq(String.class));

        lambdaCaptor.getValue().apply(context);
        verify(messagingService, times(1)).sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName),
                                                                      eq(fakeIpAddress), eq(fakeOrionGuid),
                                                                      eq(VmMetric.DISK.name()), eq("42%"),
                                                                      any(Instant.class), eq(true));
    }

    @Test
    public void passesDefaultValueToOutageEmail() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.DISK);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.reason = "unexpected string";

        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-DISK"), lambdaCaptor.capture(), eq(String.class));

        lambdaCaptor.getValue().apply(context);
        verify(messagingService, times(1)).sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName),
                                                                      eq(fakeIpAddress), eq(fakeOrionGuid),
                                                                      eq(VmMetric.DISK.name()), eq("95%"),
                                                                      any(Instant.class), eq(true));
    }

    @Test
    public void handlesOutageWithMultipleMetrics() {
        vmOutageEmailRequest.vmOutage.metrics = new HashSet<>();
        vmOutageEmailRequest.vmOutage.metrics.add(VmMetric.HTTP);
        vmOutageEmailRequest.vmOutage.metrics.add(VmMetric.IMAP);
        vmOutageEmailRequest.vmOutage.metrics.add(VmMetric.SSH);
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("Unable to resolve host name domainfake.here"), VmMetric.HTTP
        ));
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.reason = "unexpected string";
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);

        command.execute(context, vmOutageEmailRequest);

        verify(context).execute(eq("SendVmOutageEmail-HTTP"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
        verify(context).execute(eq("SendVmOutageEmail-IMAP"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
        verify(context).execute(eq("SendVmOutageEmail-SSH"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
    }
}