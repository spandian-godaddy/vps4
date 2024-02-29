package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.hfs.plesk.PleskAdminPassRequest;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskService;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateAdminPasswordTest {

    PleskService pleskService = mock(PleskService.class);
    WaitForPleskAction waitAction = mock(WaitForPleskAction.class);
    Cryptography cryptography = mock(Cryptography.class);

    UpdateAdminPassword command = new UpdateAdminPassword(pleskService, cryptography);
    @Captor
    private ArgumentCaptor<PleskAdminPassRequest> updateAdminPasswordCaptor;

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
        when(pleskService.adminPassUpdate(any())).thenReturn(pleskAction);
        when(cryptography.decrypt(request.encryptedPassword)).thenReturn(password);

        command.execute(context, request);

        verify(pleskService, times(1)).adminPassUpdate(updateAdminPasswordCaptor.capture());
        verify(context, times(1)).execute(WaitForPleskAction.class, pleskAction);
        PleskAdminPassRequest updateAdminPassRequest = updateAdminPasswordCaptor.getValue();
        assertEquals(password, updateAdminPassRequest.pleskAdminPass);
        assertEquals(request.vmId, updateAdminPassRequest.serverId);

    }

    @Test(expected = RuntimeException.class)
    public void failUpdateAdminPassword() {
        UpdateAdminPassword.Request request = new UpdateAdminPassword.Request();
        request.vmId = 42;
        request.encryptedPassword = "password".getBytes();

        // if HFS throws an exception on pleskService, the command should fail
        when(pleskService.adminPassUpdate(any())).thenThrow(new RuntimeException("Faked HFS failure"));

        command.execute(context, request);
    }


}
