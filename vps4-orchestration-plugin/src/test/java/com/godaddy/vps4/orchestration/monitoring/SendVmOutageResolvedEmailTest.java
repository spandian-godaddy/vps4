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
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class SendVmOutageResolvedEmailTest {

    Vps4MessagingService vps4MessagingService = mock(Vps4MessagingService.class);
    VmAlertService vmAlertService = mock(VmAlertService.class);
    VmOutageEmailRequest vmOutageEmailRequest = new VmOutageEmailRequest();
    VmOutage vmOutage = new VmOutage();
    VmMetricAlert vmMetricAlert = new VmMetricAlert();
    CommandContext context = mock(CommandContext.class);
    String fakeMessageId = "fake-message-id";
    UUID fakeCustomerId = UUID.randomUUID();
    String fakeAccountName = "fake-account-name";
    String fakeIpAddress = "127.0.0.1";
    UUID fakeOrionGuid = UUID.randomUUID();
    String fakeReason = "fake-reason";
    UUID fakeVmId = UUID.randomUUID();

    SendVmOutageResolvedEmail command = new SendVmOutageResolvedEmail(vps4MessagingService, vmAlertService);

    @Captor
    ArgumentCaptor<Function<CommandContext, String>> lambdaCaptor;

    @Before
    public void setUp() {
        Instant started = Instant.now();
        Instant ended = Instant.now();
        vmOutageEmailRequest.managed = true;
        vmOutageEmailRequest.customerId = fakeCustomerId;
        vmOutageEmailRequest.vmId = fakeVmId;
        vmOutageEmailRequest.accountName = fakeAccountName;
        vmOutageEmailRequest.ipAddress = fakeIpAddress;
        vmOutageEmailRequest.orionGuid = fakeOrionGuid;
        vmOutageEmailRequest.vmOutage = vmOutage;

        vmOutage.started = started;
        vmOutage.ended = ended;
        vmOutage.reason = fakeReason;
        vmOutage.domainMonitoringMetadata = new ArrayList<>();
    }

    @Test
    public void verifyUptimeOutageResolvedEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.PING);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);
        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                     .sendUptimeOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                                                    eq(fakeOrionGuid), any(Instant.class), eq(true)))
                .thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-PING"), lambdaCaptor.capture(),
                         eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServerUsageOutageResolvedEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.CPU);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);
        String fakeResourceName = VmMetric.CPU.name();
        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                     .sendUsageOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                                                   eq(fakeOrionGuid), eq(fakeResourceName), any(Instant.class),
                                                   eq(true)))
                .thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-CPU"), lambdaCaptor.capture(),
                         eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesRestoredEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.FTP);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);
        String fakeResourceName = VmMetric.FTP.name();
        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                .sendServiceOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq(fakeResourceName), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-FTP"), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesRestoredEmailIsSentHTTP() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTP);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("Unable to resolve host name domainfake.here"), VmMetric.HTTP
        ));
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                     .sendServiceOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                                                     eq(fakeOrionGuid), eq("HTTP (domainfake.here)"), any(Instant.class),
                                                     eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-HTTP"), lambdaCaptor.capture(),
                         eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesRestoredEmailIsSentHTTPS() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL error: certificate verify failed"), VmMetric.HTTPS
        ));
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                .sendServiceOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq("HTTPS (domainfake.here)"), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-HTTPS"), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void sendsOutageForResolvedEmailMultipleReasons() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here",
                Arrays.asList("SSL certificate is expiring", "SSL error: certificate verify failed"),
                VmMetric.HTTPS
        ));
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                .sendServiceOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq("HTTPS (domainfake.here)"), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageResolvedEmail-HTTPS"), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void doesNotSendOutageResolvedEmailForSSLWarning() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.HTTPS);
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.domainMonitoringMetadata = Arrays.asList(new VmOutage.DomainMonitoringMetadata(
                "domainfake.here", Arrays.asList("SSL certificate is expiring"), VmMetric.HTTPS
        ));
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);

        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vps4MessagingService
                .sendServiceOutageResolvedEmail(eq(fakeCustomerId), eq(fakeAccountName), eq(fakeIpAddress),
                        eq(fakeOrionGuid), eq("HTTPS (domainfake.here)"), any(Instant.class),
                        eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(0))
                .execute(eq("SendVmOutageResolvedEmail-HTTPS"), lambdaCaptor.capture(),
                        eq(String.class));
    }


    @Test
    public void doesNotSendEmailIfAlertMetricIsDisabled() {
        vmOutageEmailRequest.vmOutage.metrics = Collections.singleton(VmMetric.PING);
        vmMetricAlert.status = VmMetricAlert.Status.DISABLED;
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);
        command.execute(context, vmOutageEmailRequest);

        verify(context, never())
                .execute(eq("SendVmOutageResolvedEmail-" + fakeCustomerId), any(Function.class),
                         eq(String.class));
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

        verify(context).execute(eq("SendVmOutageResolvedEmail-HTTP"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
        verify(context).execute(eq("SendVmOutageResolvedEmail-IMAP"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
        verify(context).execute(eq("SendVmOutageResolvedEmail-SSH"), Matchers.<Function<CommandContext, String>> any(), eq(String.class));
    }
}