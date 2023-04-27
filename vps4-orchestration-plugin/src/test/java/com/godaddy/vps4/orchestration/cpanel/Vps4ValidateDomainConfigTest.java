package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4ValidateDomainConfigTest {
    Vps4CpanelService cPanelService = mock(Vps4CpanelService.class);
    CommandContext context = mock(CommandContext.class);
    Vps4ValidateDomainConfig.Request req;
    UUID vmId = UUID.randomUUID();
    long hfsVmId = 42L;
    String allowRemoteDomainsKey = "allowremotedomains";
    String allowUnregisteredDomainsKey = "allowunregistereddomains";

    @Captor private ArgumentCaptor<Long> hfsVmIdArgumentCaptor;
    @Captor private ArgumentCaptor<String> keyArgumentCaptor;
    @Captor private ArgumentCaptor<String> valueArgumentCaptor;

    Vps4ValidateDomainConfig command;
    @Before
    public void setUp() throws CpanelTimeoutException, CpanelAccessDeniedException, IOException {
        req = new Vps4ValidateDomainConfig.Request();
        req.vmId = vmId;
        req.hfsVmId = hfsVmId;

        command = new Vps4ValidateDomainConfig(cPanelService);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void callsGetTweakSettingsWithCorrectParameters() throws CpanelTimeoutException, CpanelAccessDeniedException {
        when(cPanelService.getTweakSettings(eq(hfsVmId), anyString())).thenReturn("1");
        command.execute(context, req);

        verify(cPanelService, times(2)).getTweakSettings(
                hfsVmIdArgumentCaptor.capture(), keyArgumentCaptor.capture());

        List<Long> capturedHfsVmIds = hfsVmIdArgumentCaptor.getAllValues();
        Assert.assertEquals(hfsVmId, (long) capturedHfsVmIds.get(0));
        Assert.assertEquals(hfsVmId, (long) capturedHfsVmIds.get(1));

        List<String> capturedKeys = keyArgumentCaptor.getAllValues();
        Assert.assertEquals(allowRemoteDomainsKey, capturedKeys.get(0));
        Assert.assertEquals(allowUnregisteredDomainsKey, capturedKeys.get(1));
    }

    @Test
    public void throwsRuntimeExceptionIfCPanelExceptionOccursDuringGetTweakSettings()
            throws CpanelTimeoutException, CpanelAccessDeniedException {
        String expectedException = "Failed to retrieve the tweak setting for " + allowRemoteDomainsKey + " for VM: " + vmId;
        when(cPanelService.getTweakSettings(eq(hfsVmId), anyString())).thenThrow(new CpanelTimeoutException("Test exception"));

        try {
            command.execute(context, req);
        } catch (RuntimeException e) {
            Assert.assertEquals(expectedException, e.getMessage());
        }
    }

    @Test
    public void callsSetTweakSettingsWithCorrectParameters() throws CpanelTimeoutException, CpanelAccessDeniedException {
        when(cPanelService.getTweakSettings(eq(hfsVmId), anyString())).thenReturn("0");
        String expectedValue = "1";

        command.execute(context, req);

        verify(cPanelService, times(2)).setTweakSettings(
                hfsVmIdArgumentCaptor.capture(), keyArgumentCaptor.capture(), valueArgumentCaptor.capture());

        List<Long> capturedHfsVmIds = hfsVmIdArgumentCaptor.getAllValues();
        Assert.assertEquals(hfsVmId, (long) capturedHfsVmIds.get(0));
        Assert.assertEquals(hfsVmId, (long) capturedHfsVmIds.get(1));

        List<String> capturedKeys = keyArgumentCaptor.getAllValues();
        Assert.assertEquals(allowRemoteDomainsKey, capturedKeys.get(0));
        Assert.assertEquals(allowUnregisteredDomainsKey, capturedKeys.get(1));

        List<String> capturedValues = valueArgumentCaptor.getAllValues();
        Assert.assertEquals(expectedValue, capturedValues.get(0));
        Assert.assertEquals(expectedValue, capturedValues.get(1));
    }

    @Test
    public void throwsRuntimeExceptionIfCPanelExceptionOccursDuringSetTweakSettings()
            throws CpanelTimeoutException, CpanelAccessDeniedException {
        String expectedException = "Failed to update the tweak setting for " + allowRemoteDomainsKey + " for VM: " + vmId;
        when(cPanelService.getTweakSettings(eq(hfsVmId), anyString())).thenReturn("0");
        when(cPanelService.setTweakSettings(eq(hfsVmId), anyString(), anyString())).thenThrow(new CpanelTimeoutException("Test exception"));

        try {
            command.execute(context, req);
        } catch (RuntimeException e) {
            Assert.assertEquals(expectedException, e.getMessage());
        }
    }
}
