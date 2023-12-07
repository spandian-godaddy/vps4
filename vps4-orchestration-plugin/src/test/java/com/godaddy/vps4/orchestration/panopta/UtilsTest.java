package com.godaddy.vps4.orchestration.panopta;

import static com.godaddy.vps4.orchestration.panopta.Utils.getNonManagedTemplateId;
import static com.godaddy.vps4.orchestration.panopta.Utils.getServerTemplateId;
import static com.godaddy.vps4.orchestration.panopta.Utils.getTemplateIds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {
    @Mock Config config;
    @Mock VirtualMachineCredit credit;

    @Before
    public void setUp() {
        when(config.get("panopta.api.templates.base.linux")).thenReturn("test-template-base-linux");
        when(config.get("panopta.api.templates.base.windows")).thenReturn("test-template-base-windows");
        when(config.get("panopta.api.templates.addon.linux")).thenReturn("test-template-addon-windows");
        when(config.get("panopta.api.templates.managed.linux")).thenReturn("test-template-managed-windows");
        when(config.get("panopta.api.templates.webhook")).thenReturn("dc-alert-template");
        when(credit.getOperatingSystem()).thenReturn("LINUX");
        when(credit.isManaged()).thenReturn(false);
    }

    @Test
    public void testServerTemplateId() {
        String result = getServerTemplateId(config, credit);
        assertSame("test-template-base-linux", result);
    }

    @Test
    public void testServerTemplateIdWithWindows() {
        when(credit.getOperatingSystem()).thenReturn("WINDOWS");
        String result = getServerTemplateId(config, credit);
        assertSame("test-template-base-windows", result);
    }

    @Test
    public void testServerTemplateIdWithAddOn() {
        when(credit.hasMonitoring()).thenReturn(true);
        String result = getServerTemplateId(config, credit);
        assertSame("test-template-addon-windows", result);
    }

    @Test
    public void testServerTemplateIdWithManaged() {
        when(credit.isManaged()).thenReturn(true);
        String result = getServerTemplateId(config, credit);
        assertSame("test-template-managed-windows", result);
    }

    @Test
    public void testDcAlertTemplate() {
        String[] result = getTemplateIds(config, credit);
        verify(config, times(1)).get("panopta.api.templates.base.linux");
        verify(config, times(1)).get("panopta.api.templates.webhook");
        assertEquals(2, result.length);
        assertSame("test-template-base-linux", result[0]);
        assertSame("dc-alert-template", result[1]);
    }

    @Test
    public void testGetNonManagedTemplateIdForBase() {
        // Setting managed to true since we use this to get the non-managed template to remove.
        when(credit.isManaged()).thenReturn(true);

        String result = getNonManagedTemplateId(config, credit);

        assertSame("test-template-base-linux", result);
    }

    @Test
    public void testGetNonManagedTemplateIdForAddon() {
        // Setting managed to true since we use this to get the non-managed template to remove.
        when(credit.isManaged()).thenReturn(true);
        when(credit.hasMonitoring()).thenReturn(true);

        String result = getNonManagedTemplateId(config, credit);

        assertSame("test-template-addon-windows", result);
    }
}
