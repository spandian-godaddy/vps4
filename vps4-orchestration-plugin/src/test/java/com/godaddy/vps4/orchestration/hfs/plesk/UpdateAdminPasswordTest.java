package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.mockito.Mockito.*;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class UpdateAdminPasswordTest {

    PleskService pleskService = mock(PleskService.class);
    WaitForPleskAction waitAction = mock(WaitForPleskAction.class);

    UpdateAdminPassword command = new UpdateAdminPassword(pleskService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UpdateAdminPassword.class);
        binder.bind(WaitForPleskAction.class).toInstance(waitAction);
        binder.bind(PleskService.class).toInstance(pleskService);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteSuccess() throws Exception {
        UpdateAdminPassword.Request request = new UpdateAdminPassword.Request();
        request.vmId = 42;
        request.password = "pleskpassword";

        PleskAction pleskAction = mock(PleskAction.class);
        when(pleskService.adminPassUpdate(request.vmId, request.password)).thenReturn(pleskAction);

        command.execute(context, request);

        verify(pleskService, times(1)).adminPassUpdate(request.vmId, request.password);
        verify(context, times(1)).execute(WaitForPleskAction.class, pleskAction);
    }

    @Test(expected = RuntimeException.class)
    public void failUpdateAdminPassword() throws Exception {
        UpdateAdminPassword.Request request = new UpdateAdminPassword.Request();
        request.vmId = 42;
        request.password = "pleskpassword";

        // if HFS throws an exception on pleskService, the command should fail
        when(pleskService.adminPassUpdate(request.vmId, request.password)).thenThrow(new RuntimeException("Faked HFS failure"));

        command.execute(context, request);
    }


}
