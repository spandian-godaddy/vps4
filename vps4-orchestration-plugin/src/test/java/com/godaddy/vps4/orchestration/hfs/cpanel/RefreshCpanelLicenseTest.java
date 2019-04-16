package com.godaddy.vps4.orchestration.hfs.cpanel;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;

public class RefreshCpanelLicenseTest {
    CPanelService cpanelService = mock(CPanelService.class);
    WaitForCpanelAction waitCpanelCmd = mock(WaitForCpanelAction.class);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForCpanelAction.class).toInstance(waitCpanelCmd);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    RefreshCpanelLicense command = new RefreshCpanelLicense(cpanelService);
    RefreshCpanelLicense.Request request;
    CPanelAction cpanelWaitAction;
    long hfsVmId = 42L;

    @Before
    public void setUp() {
        request = new RefreshCpanelLicense.Request();
        request.hfsVmId = hfsVmId;
        cpanelWaitAction = mock(CPanelAction.class);
        cpanelWaitAction.status = CPanelAction.Status.COMPLETE;
        doReturn(cpanelWaitAction).when(context).execute(eq("WaitForLicenseRefresh"), eq(WaitForCpanelAction.class), any());
    }

    @Test
    public void executesLicenseRefresh() {
        command.execute(context, request);
        verify(cpanelService).licenseRefresh(hfsVmId);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executesWaitForLicenseRefresh() {
        CPanelAction refreshAction = mock(CPanelAction.class);
        doReturn(refreshAction).when(context).execute(eq("RefreshCPanelLicense"), any(Function.class), any());
        command.execute(context, request);
        verify(context).execute("WaitForLicenseRefresh", WaitForCpanelAction.class, refreshAction);
    }

    @Test(expected=RuntimeException.class)
    public void throwsExceptionOnFailedStatus() {
        cpanelWaitAction.status = CPanelAction.Status.FAILED;
        command.execute(context, request);
        fail();
    }

}
