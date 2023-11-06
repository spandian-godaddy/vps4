package com.godaddy.vps4.orchestration.hfs.plesk;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.plesk.PleskService;
import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskAction.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SetPleskOutgoingEmailIpTest {

    PleskService pleskService;
    SetPleskOutgoingEmailIp command;
    CommandContext context;
    Cryptography cryptography;
    Injector injector;

    @Before
    public void setup() throws Exception {

        pleskService = mock(PleskService.class);
        cryptography = mock(Cryptography.class);
        command = new SetPleskOutgoingEmailIp(pleskService);
        injector = Guice.createInjector(binder -> {
            binder.bind(SetPleskOutgoingEmailIp.class);
            binder.bind(WaitForPleskAction.class);
            binder.bind(PleskService.class).toInstance(pleskService);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    @After
    public void tearDown() throws Exception {
        pleskService = null;
        command = null;
        injector = null;
        context = null;
    }

    @Test
    public void testExecuteSuccess() {
        String password = "super-secret-password";
        when(cryptography.decrypt(any())).thenReturn(password);
        SetPleskOutgoingEmailIp.SetPleskOutgoingEmailIpRequest request = new SetPleskOutgoingEmailIp.SetPleskOutgoingEmailIpRequest
                (777L, "192.168.0.1");

        PleskAction pleskAction = new PleskAction();
        pleskAction.actionId = 555;
        pleskAction.status = Status.COMPLETE;

        when(pleskService.setOutgoingEMailIP(request.hfsVmId, request.ipAddress)).thenReturn(pleskAction);
        when(pleskService.getAction(pleskAction.actionId)).thenReturn(pleskAction);

        command.execute(context, request);

        verify(pleskService, times(1)).setOutgoingEMailIP(request.hfsVmId, request.ipAddress);
    }

    @Test(expected = RuntimeException.class)
    public void failSetPleskOutgoingEmailIp() {
        SetPleskOutgoingEmailIp.SetPleskOutgoingEmailIpRequest request =
                new SetPleskOutgoingEmailIp.SetPleskOutgoingEmailIpRequest(777L, "192.168.0.1");

        // if HFS throws an exception on pleskService, the command should fail
        when(pleskService.setOutgoingEMailIP(request.hfsVmId, request.ipAddress)).thenThrow(new RuntimeException("Faked an HFS failure"));

        command.execute(context, request);
    }
}
