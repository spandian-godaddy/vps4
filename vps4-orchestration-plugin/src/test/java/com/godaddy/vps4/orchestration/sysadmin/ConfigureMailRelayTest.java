package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.vm.Image.ControlPanel;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.sysadmin.SysAdminAction;
import com.godaddy.hfs.sysadmin.SysAdminAction.Status;
import com.godaddy.hfs.sysadmin.SysAdminService;

public class ConfigureMailRelayTest {

    SysAdminService sysAdminService = mock(SysAdminService.class);

    ConfigureMailRelay command = new ConfigureMailRelay(sysAdminService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ConfigureMailRelay.class);
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testExecuteConfigMailRelaySuccess() {
        ConfigureMailRelayRequest request = new ConfigureMailRelayRequest(777L, ControlPanel.CPANEL);

        SysAdminAction sysAdminAction = new SysAdminAction();
        sysAdminAction.sysAdminActionId = 123;
        sysAdminAction.status = Status.COMPLETE;

        when(sysAdminService.configureMTA(request.vmId, request.controlPanel)).thenReturn(sysAdminAction);
        when(sysAdminService.getSysAdminAction(sysAdminAction.sysAdminActionId)).thenReturn(sysAdminAction);

        command.execute(context, request);

        verify(sysAdminService, times(1)).configureMTA(request.vmId, request.controlPanel);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteConfigMailRelayFail() {
        ConfigureMailRelayRequest request = new ConfigureMailRelayRequest(777L, ControlPanel.CPANEL);
        
        when(sysAdminService.configureMTA(request.vmId, request.controlPanel)).thenThrow(new RuntimeException("HFS Failed"));
        
        command.execute(context, request);
        
    }
}
