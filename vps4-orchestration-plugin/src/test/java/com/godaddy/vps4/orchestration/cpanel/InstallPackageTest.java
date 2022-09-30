package com.godaddy.vps4.orchestration.cpanel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelBuild;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class InstallPackageTest {
    @Mock private CommandContext context;
    @Mock private Vps4CpanelService cpanelService;

    private InstallPackage installPackage;
    private InstallPackage.Request request;

    private final long hfsVmId = 42;
    private final long buildNumber = 32;
    private final String packageName =  "testPackage";

    @Captor private ArgumentCaptor<WaitForPackageInstall.Request> waitForPackageInstallReq;

    @Before
    public void setUp() throws Exception {
        installPackage = new InstallPackage(cpanelService);
        when(cpanelService.installRpmPackage(hfsVmId, packageName)).thenReturn(new CpanelBuild(buildNumber, packageName));
        when(context.execute(eq(WaitForPackageInstall.class), any())).thenReturn(null);
        request = setupRequest();
    }

    private InstallPackage.Request setupRequest() {
        InstallPackage.Request request = new InstallPackage.Request();
        request.hfsVmId = hfsVmId;
        request.packageName = packageName;
        return request;
    }

    @Test
    public void installsPackage() throws CpanelTimeoutException, CpanelAccessDeniedException {
        installPackage.execute(context, request);
        verify(cpanelService, times(1)).installRpmPackage(hfsVmId, packageName);
    }

    @Test(expected = RuntimeException.class)
    public void throwsRuntimeException() throws CpanelTimeoutException, CpanelAccessDeniedException {
        when(cpanelService.installRpmPackage(hfsVmId, packageName)).thenThrow(new CpanelTimeoutException("test"));
        installPackage.execute(context, request);
    }

    @Test
    public void waitForPackageInstall() {
        installPackage.execute(context, request);
        verify(context, times(1)).execute(eq(WaitForPackageInstall.class), waitForPackageInstallReq.capture());
        WaitForPackageInstall.Request req = waitForPackageInstallReq.getValue();
        assertEquals(hfsVmId, req.hfsVmId);
        assertEquals(buildNumber, req.buildNumber);
    }
}