package com.godaddy.vps4.orchestration.panopta;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class GetPanoptaServerKeyFromHfsTest {

    private GetPanoptaServerKeyFromHfs command;
    private CommandContext contextMock;
    private SysAdminAction sysAdminActionMock;
    private long fakeHfsVmId = 1234L;

    @Before
    public void setUp() throws Exception {
        contextMock = mock(CommandContext.class);
        sysAdminActionMock = mock(SysAdminAction.class);
        SysAdminService sysAdminServiceMock = mock(SysAdminService.class);

        command = new GetPanoptaServerKeyFromHfs(sysAdminServiceMock);
        when(contextMock.execute(eq("HfsGetPanoptaServerKey"), any(Function.class),
                                 eq(SysAdminAction.class))).thenReturn(sysAdminActionMock);
        when(contextMock.execute(eq("WaitForPanoptaServerKeyFromHfs-" + fakeHfsVmId), eq(WaitForSysAdminAction.class),
                                 eq(sysAdminActionMock))).thenReturn(sysAdminActionMock);
    }

    @Test
    public void invokesGetPanoptaServerKey() {
        SysAdminAction actualSysAdminAction = command.execute(contextMock, fakeHfsVmId);

        verify(contextMock).execute(eq("HfsGetPanoptaServerKey"),
                                    any(Function.class), eq(SysAdminAction.class));
        verify(contextMock).execute(eq("WaitForPanoptaServerKeyFromHfs-" + fakeHfsVmId),
                                    eq(WaitForSysAdminAction.class), eq(sysAdminActionMock));
        assertEquals(sysAdminActionMock, actualSysAdminAction);
    }
}