package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskAction.Status;
import com.godaddy.hfs.plesk.PleskImageConfigRequest;
import com.godaddy.hfs.plesk.PleskService;
import com.godaddy.vps4.vm.PleskLicenseType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurePleskTest {

    PleskService pleskService;
    ConfigurePlesk command;
    CommandContext context;
    Cryptography cryptography;
    Injector injector;

    @Captor
    private ArgumentCaptor<PleskImageConfigRequest> pleskImageConfigCaptor;

    @Before
    public void setup() throws Exception {

        pleskService = mock(PleskService.class);
        cryptography = mock(Cryptography.class);
        command = new ConfigurePlesk(pleskService, cryptography);
        injector = Guice.createInjector(binder -> {
            binder.bind(ConfigurePlesk.class);
            binder.bind(WaitForPleskAction.class);
            binder.bind(PleskService.class).toInstance(pleskService);
            binder.bind(Cryptography.class).toInstance(cryptography);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }


    @After
    public void tearDown() {
        pleskService = null;
        command = null;
        injector = null;
        context = null;
    }

    @Test
    public void testExecuteSuccess() {
        String password = "super-secret-password";
        when(cryptography.decrypt(any())).thenReturn(password);
        ConfigurePleskRequest request = new ConfigurePleskRequest(777L, "fake-user", password.getBytes(), PleskLicenseType.PLESK);

        PleskAction pleskAction = new PleskAction();
        pleskAction.actionId = 555;
        pleskAction.status = Status.COMPLETE;

        when(pleskService.imageConfig(any())).thenReturn(pleskAction);
        when(pleskService.getAction(pleskAction.actionId)).thenReturn(pleskAction);

        command.execute(context, request);

        verify(pleskService, times(1)).imageConfig(pleskImageConfigCaptor.capture());

        PleskImageConfigRequest req = pleskImageConfigCaptor.getValue();
        assertEquals(request.vmId, req.serverId);
        assertEquals(request.username, req.pleskUser);
        assertEquals(password, req.pleskPass);
        assertEquals("web_host", req.pleskLicenseType);
    }

    @Test
    public void testExecuteSuccessWebHost() {
        String password = "super-secret-password";
        when(cryptography.decrypt(any())).thenReturn(password);
        ConfigurePleskRequest request = new ConfigurePleskRequest(777L, "fake-user", password.getBytes(), PleskLicenseType.PLESKWEBHOST);

        PleskAction pleskAction = new PleskAction();
        pleskAction.actionId = 555;
        pleskAction.status = Status.COMPLETE;

        when(pleskService.imageConfig(any())).thenReturn(pleskAction);
        when(pleskService.getAction(pleskAction.actionId)).thenReturn(pleskAction);

        command.execute(context, request);

        verify(pleskService, times(1)).imageConfig(pleskImageConfigCaptor.capture());

        PleskImageConfigRequest req = pleskImageConfigCaptor.getValue();
        assertEquals(request.vmId, req.serverId);
        assertEquals(request.username, req.pleskUser);
        assertEquals(password, req.pleskPass);
        assertEquals("web_host", req.pleskLicenseType);
    }

    @Test
    public void testExecuteSuccessWebPro() {
        String password = "super-secret-password";
        when(cryptography.decrypt(any())).thenReturn(password);
        ConfigurePleskRequest request = new ConfigurePleskRequest(777L, "fake-user", password.getBytes(), PleskLicenseType.PLESKWEBPRO);

        PleskAction pleskAction = new PleskAction();
        pleskAction.actionId = 555;
        pleskAction.status = Status.COMPLETE;

        when(pleskService.imageConfig(any())).thenReturn(pleskAction);
        when(pleskService.getAction(pleskAction.actionId)).thenReturn(pleskAction);

        command.execute(context, request);

        verify(pleskService, times(1)).imageConfig(pleskImageConfigCaptor.capture());

        PleskImageConfigRequest req = pleskImageConfigCaptor.getValue();
        assertEquals(request.vmId, req.serverId);
        assertEquals(request.username, req.pleskUser);
        assertEquals(password, req.pleskPass);
        assertEquals("web_pro", req.pleskLicenseType);

    }

    @Test(expected = RuntimeException.class)
    public void failPleskImageConfig() {
        ConfigurePleskRequest request = new ConfigurePleskRequest(777L, "fake-user", "super-secret-password".getBytes(), PleskLicenseType.PLESK);

        // if HFS throws an exception on pleskService, the command should fail
        when(pleskService.imageConfig(any())).thenThrow(new RuntimeException("Faked an HFS failure"));

        command.execute(context, request);
    }
}
