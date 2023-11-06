package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskService;

public class UpdateAdminPasswordTest {

    PleskService pleskService = mock(PleskService.class);
    WaitForPleskAction waitAction = mock(WaitForPleskAction.class);
    Cryptography cryptography = mock(Cryptography.class);

    UpdateAdminPassword command = new UpdateAdminPassword(pleskService, cryptography);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UpdateAdminPassword.class);
        binder.bind(WaitForPleskAction.class).toInstance(waitAction);
        binder.bind(PleskService.class).toInstance(pleskService);
        binder.bind(Cryptography.class).toInstance(cryptography);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteSuccess() throws Exception {
        UpdateAdminPassword.Request request = new UpdateAdminPassword.Request();
        request.vmId = 42;
        String password = "password";
        request.encryptedPassword = password.getBytes();

        PleskAction pleskAction = mock(PleskAction.class);
        when(pleskService.adminPassUpdate(request.vmId, password)).thenReturn(pleskAction);
        when(cryptography.decrypt(request.encryptedPassword)).thenReturn(password);

        command.execute(context, request);

        verify(pleskService, times(1)).adminPassUpdate(request.vmId, password);
        verify(context, times(1)).execute(WaitForPleskAction.class, pleskAction);
    }

    @Test(expected = RuntimeException.class)
    public void failUpdateAdminPassword() throws Exception {
        UpdateAdminPassword.Request request = new UpdateAdminPassword.Request();
        request.vmId = 42;
        request.encryptedPassword = "password".getBytes();

        // if HFS throws an exception on pleskService, the command should fail
        when(pleskService.adminPassUpdate(request.vmId, anyString())).thenThrow(new RuntimeException("Faked HFS failure"));

        command.execute(context, request);
    }


}
