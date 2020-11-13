package com.godaddy.vps4.orchestration.hfs.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

@RunWith(MockitoJUnitRunner.class)
public class InstallPanoptaTest {

    private SysAdminService sysAdminService;
    private InstallPanopta command;
    private CommandContext context;
    private InstallPanopta.Request request;
    private SysAdminAction dummyHfsAction;
    private long fakeHfsVmId = 1234L;
    private String fakeCustomerKey = "totally-fake-customer-key";
    private String fakeTemplates = "totally-fake-templates";
    private String fakeServerKey = "totally-fake-server-key";
    private String fakeFqdn = "totally-fake-fqdn";
    private UUID fakeOrionGUID = UUID.randomUUID();
    private String fakeServerName = fakeOrionGUID.toString();
    private boolean fakeDisableServerMatch = false;

    @Captor
    private ArgumentCaptor<Function<CommandContext, SysAdminAction>> panoptaInstallArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        sysAdminService = mock(SysAdminService.class);
        command = new InstallPanopta(sysAdminService);
        context = mock(CommandContext.class);
        dummyHfsAction = mock(SysAdminAction.class);
        setupMockContext();
        setupCommandRequest();
    }

    private void setupMockContext() {
        when(context.execute(eq("InstallPanopta-" + fakeHfsVmId), any(Function.class), eq(SysAdminAction.class)))
                .thenReturn(dummyHfsAction);
        when(sysAdminService.installPanopta(fakeHfsVmId, fakeCustomerKey, fakeTemplates, fakeServerName, fakeServerKey, fakeFqdn,
                fakeDisableServerMatch))
                .thenReturn(dummyHfsAction);
    }

    private void setupCommandRequest() {
        request = new InstallPanopta.Request();
        request.hfsVmId = fakeHfsVmId;
        request.customerKey = fakeCustomerKey;
        request.templates = fakeTemplates;
        request.serverKey = fakeServerKey;
        request.fqdn = fakeFqdn;
        request.serverName = fakeServerName;
        request.disableServerMatch = false;
    }

    @Test
    public void invokesPanoptaInstallation() {
        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("InstallPanopta-" + fakeHfsVmId), panoptaInstallArgumentCaptor.capture(),
                        eq(SysAdminAction.class));
        Function<CommandContext, SysAdminAction> lambdaValue = panoptaInstallArgumentCaptor.getValue();
        SysAdminAction sysAdminAction = lambdaValue.apply(context);
        verify(sysAdminService, times(1)).installPanopta(fakeHfsVmId, fakeCustomerKey, fakeTemplates, fakeServerName,
                fakeServerKey, fakeFqdn, fakeDisableServerMatch);
        assertEquals(dummyHfsAction, sysAdminAction);
    }
}