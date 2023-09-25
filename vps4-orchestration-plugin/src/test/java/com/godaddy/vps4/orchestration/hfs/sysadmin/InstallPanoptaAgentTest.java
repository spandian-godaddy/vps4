package com.godaddy.vps4.orchestration.hfs.sysadmin;

import static org.junit.Assert.assertEquals;
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
public class InstallPanoptaAgentTest {

    private SysAdminService sysAdminService;
    private InstallPanoptaAgent command;
    private CommandContext context;
    private InstallPanoptaAgent.Request request;
    private SysAdminAction dummyHfsAction;
    private final long fakeHfsVmId = 1234L;
    private final String fakeCustomerKey = "totally-fake-customer-key";
    private final String fakeTemplates = "totally-fake-templates";
    private final String fakeServerKey = "totally-fake-server-key";
    private final String fakeFqdn = "totally-fake-fqdn";
    private final UUID fakeOrionGUID = UUID.randomUUID();
    private final String fakeServerName = fakeOrionGUID.toString();

    @Captor
    private ArgumentCaptor<Function<CommandContext, SysAdminAction>> panoptaInstallArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        sysAdminService = mock(SysAdminService.class);
        command = new InstallPanoptaAgent(sysAdminService);
        context = mock(CommandContext.class);
        dummyHfsAction = mock(SysAdminAction.class);
        setupMockContext();
        setupCommandRequest();
    }

    private void setupMockContext() {
        when(sysAdminService.installPanopta(
                fakeHfsVmId, fakeCustomerKey, fakeTemplates, fakeServerName, fakeServerKey, fakeFqdn, false
        )).thenReturn(dummyHfsAction);
    }

    private void setupCommandRequest() {
        request = new InstallPanoptaAgent.Request();
        request.hfsVmId = fakeHfsVmId;
        request.customerKey = fakeCustomerKey;
        request.templateIds = fakeTemplates;
        request.serverKey = fakeServerKey;
        request.fqdn = fakeFqdn;
        request.serverName = fakeServerName;
    }

    @Test
    public void invokesPanoptaInstallation() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("InstallPanoptaAgent"),
                                          panoptaInstallArgumentCaptor.capture(),
                                          eq(SysAdminAction.class));
        Function<CommandContext, SysAdminAction> lambdaValue = panoptaInstallArgumentCaptor.getValue();
        SysAdminAction sysAdminAction = lambdaValue.apply(context);
        verify(sysAdminService, times(1)).installPanopta(fakeHfsVmId, fakeCustomerKey, fakeTemplates, fakeServerName,
                                                         fakeServerKey, fakeFqdn, false);
        assertEquals(dummyHfsAction, sysAdminAction);
    }
}