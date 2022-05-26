package com.godaddy.vps4.orchestration.hfs.cpanel;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelLicense;
import com.godaddy.hfs.cpanel.CPanelService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class UnlicenseCpanelTest {
    CPanelService cpanelService = mock(CPanelService.class);
    WaitForCpanelAction waitCpanelCmd = mock(WaitForCpanelAction.class);
    CPanelAction cpanelAction = mock(CPanelAction.class);
    CPanelLicense cpanelLicense = new CPanelLicense();
    Long hfsVmId = 42L;

    UnlicenseCpanel command = new UnlicenseCpanel(cpanelService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForCpanelAction.class).toInstance(waitCpanelCmd);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setUp() {
        cpanelLicense.licensedIp = "127.0.0.1";

        when(cpanelService.getLicenseFromDb(hfsVmId)).thenReturn(cpanelLicense);
        when(cpanelService.licenseRelease(null, hfsVmId)).thenReturn(cpanelAction);
        doReturn(cpanelAction).when(context).execute(eq(WaitForCpanelAction.class), any());
    }

    @Test
    public void executesCpanelLicenseRelease() {
        command.execute(context, hfsVmId);
        verify(cpanelService).licenseRelease(null, hfsVmId);
    }

    @Test
    public void executesWaitForCpanelAction() {
        command.execute(context, hfsVmId);
        verify(context).execute(WaitForCpanelAction.class, cpanelAction);
    }

    @Test(expected=RuntimeException.class)
    public void throwsExceptionOnFailedLicenseRelease() {
        cpanelAction.status = CPanelAction.Status.FAILED;
        command.execute(context, hfsVmId);
        fail();
    }
}
