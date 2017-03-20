package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMta.ConfigureMtaRequest;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class ConfigureMtaTest {

    SysAdminService sysAdminService = mock(SysAdminService.class);

    ConfigureMta command = new ConfigureMta(sysAdminService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ConfigureMta.class);
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testExecuteConfigMtaSuccess() {
        ConfigureMtaRequest request = new ConfigureMtaRequest(777L, "cpanel");

        SysAdminAction sysAdminAction = new SysAdminAction();
        sysAdminAction.sysAdminActionId = 123;
        sysAdminAction.status = Status.COMPLETE;

        when(sysAdminService.configureMTA(request.vmId, request.controlPanel)).thenReturn(sysAdminAction);
        when(sysAdminService.getSysAdminAction(sysAdminAction.sysAdminActionId)).thenReturn(sysAdminAction);

        command.execute(context, request);

        verify(sysAdminService, times(1)).configureMTA(request.vmId, request.controlPanel);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteConfigMtaFail() {
        ConfigureMtaRequest request = new ConfigureMtaRequest(777L, "cpanel");
        
        when(sysAdminService.configureMTA(request.vmId, request.controlPanel)).thenThrow(new RuntimeException("HFS Failed"));
        
        command.execute(context, request);
        
    }
}
