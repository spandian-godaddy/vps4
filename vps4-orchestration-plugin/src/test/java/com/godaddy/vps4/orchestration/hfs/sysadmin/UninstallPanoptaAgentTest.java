package com.godaddy.vps4.orchestration.hfs.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
public class UninstallPanoptaAgentTest {
    private final long fakeHfsVmId = 1234L;

    private SysAdminService sysAdminService;
    private UninstallPanoptaAgent command;
    private CommandContext context;
    private SysAdminAction dummyHfsAction;

    @Captor
    private ArgumentCaptor<Function<CommandContext, SysAdminAction>> panoptaInstallArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        sysAdminService = mock(SysAdminService.class);
        command = new UninstallPanoptaAgent(sysAdminService);
        context = mock(CommandContext.class);
        dummyHfsAction = mock(SysAdminAction.class);
        when(sysAdminService.deletePanopta(fakeHfsVmId)).thenReturn(dummyHfsAction);
    }

    @Test
    public void invokesPanoptaInstallation() {
        command.execute(context, fakeHfsVmId);

        verify(context, times(1)).execute(eq("UninstallPanoptaAgent"),
                                          panoptaInstallArgumentCaptor.capture(),
                                          eq(SysAdminAction.class));
        Function<CommandContext, SysAdminAction> lambdaValue = panoptaInstallArgumentCaptor.getValue();
        SysAdminAction sysAdminAction = lambdaValue.apply(context);
        verify(sysAdminService, times(1)).deletePanopta(fakeHfsVmId);
        assertEquals(dummyHfsAction, sysAdminAction);
    }
}
