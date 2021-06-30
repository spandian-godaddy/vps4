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
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class SendVmOutageEmailTest {
    @Mock private Vps4MessagingService vps4MessagingService;
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

        setupMocks();
        command = new SendVmOutageEmail(vps4MessagingService, vmAlertService);
    }

    private void setupMocks() {
        when(context.execute(anyString(), any(Function.class), any())).thenReturn(fakeMessageId);
        when(vmAlertService.getVmMetricAlert(any(UUID.class), anyString())).thenReturn(vmMetricAlert);
    }

    @Test
    public void verifyUptimeOutageEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metric = VmMetric.PING;
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        when(vps4MessagingService
                .sendUptimeOutageEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), lambdaCaptor.capture(),
                        eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServerUsageOutageEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metric = VmMetric.CPU;
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        String fakeResourceName = VmMetric.CPU.name();
        when(vps4MessagingService.sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress),
                eq(fakeOrionGuid), eq(fakeResourceName), anyString(), any(Instant.class), eq(true)))
                .thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void verifyServicesDownEmailIsSent() {
        vmOutageEmailRequest.vmOutage.metric = VmMetric.HTTP;
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        String fakeResourceName = VmMetric.HTTP.name();
        when(vps4MessagingService
                .sendServicesDownEmail(eq(fakeShopperId), eq(fakeAccountName), eq(fakeIpAddress), eq(fakeOrionGuid),
                        eq(fakeResourceName), any(Instant.class), eq(true))).thenReturn(fakeMessageId);
        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), lambdaCaptor.capture(), eq(String.class));
        Function<CommandContext, String> lambdaValue = lambdaCaptor.getValue();
        String actualMessageId = lambdaValue.apply(context);
        assertNotNull(actualMessageId);
    }

    @Test
    public void doesNotSendEmailIfAlertMetricIsDisabled() {
        vmOutageEmailRequest.vmOutage.metric = VmMetric.PING;
        vmMetricAlert.status = VmMetricAlert.Status.DISABLED;
        command.execute(context, vmOutageEmailRequest);

        verify(context, never())
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), any(Function.class), eq(String.class));
    }

    @Test
    public void passesCorrectValueToOutageEmail() {
        vmOutageEmailRequest.vmOutage.metric = VmMetric.DISK;
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.reason = "vps4_disk_total_percent_used greater than 42% for more than 10 minutes";

        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), lambdaCaptor.capture(), eq(String.class));

        lambdaCaptor.getValue().apply(context);
        verify(vps4MessagingService, times(1)).sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName),
                                                                          eq(fakeIpAddress), eq(fakeOrionGuid),
                                                                          eq(VmMetric.DISK.name()), eq("42%"),
                                                                          any(Instant.class), eq(true));
    }

    @Test
    public void passesDefaultValueToOutageEmail() {
        vmOutageEmailRequest.vmOutage.metric = VmMetric.DISK;
        vmMetricAlert.status = VmMetricAlert.Status.ENABLED;
        vmOutageEmailRequest.vmOutage.reason = "unexpected string";

        command.execute(context, vmOutageEmailRequest);

        verify(context, times(1))
                .execute(eq("SendVmOutageEmail-" + fakeShopperId), lambdaCaptor.capture(), eq(String.class));

        lambdaCaptor.getValue().apply(context);
        verify(vps4MessagingService, times(1)).sendServerUsageOutageEmail(eq(fakeShopperId), eq(fakeAccountName),
                                                                          eq(fakeIpAddress), eq(fakeOrionGuid),
                                                                          eq(VmMetric.DISK.name()), eq("95%"),
                                                                          any(Instant.class), eq(true));
    }
}