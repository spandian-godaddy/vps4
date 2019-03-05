package com.godaddy.vps4.orchestration.hfs.sysadmin;

import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.Image;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SetPasswordTest {
    private SysAdminService sysAdminService = mock(SysAdminService.class);
    private Cryptography cryptography = mock(Cryptography.class);
    private SetPassword command;
    private Injector injector;
    private String dummyPassword = "foobar";
    private String username = "jdoe";
    private String controlPanel = Image.ISPCONFIG;
    private SysAdminAction dummyHfsAction = mock(SysAdminAction.class);
    private CommandContext context;
    private SetPassword.Request request;
    long hfsVmId = 123L;
    @Captor private ArgumentCaptor<Function<CommandContext, SysAdminAction>> changePasswordLambdaCaptor;

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(SysAdminService.class).toInstance(sysAdminService);
                bind(Cryptography.class).toInstance(cryptography);
                bind(SetPassword.class);
            }
        });
        MockitoAnnotations.initMocks(this);

        when(cryptography.decrypt(any())).thenReturn(dummyPassword);
        setupMockContext();
        setupCommandRequest();

        command = injector.getInstance(SetPassword.class);
    }

    private void setupCommandRequest() {
        request = new SetPassword.Request();
        request.hfsVmId = hfsVmId;
        request.controlPanel = controlPanel;
        request.encryptedPassword = dummyPassword.getBytes();
        request.usernames = Arrays.asList(username);
    }

    private void setupMockContext() {
        context = mock(CommandContext.class);
        when(context.execute(eq("SetPassword-" + username), any(Function.class), eq(SysAdminAction.class)))
            .thenReturn(dummyHfsAction);
        when(sysAdminService.changePassword(anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(dummyHfsAction);
        when(sysAdminService.changePassword(anyLong(), anyString(), anyString(), eq(null)))
                .thenReturn(dummyHfsAction);
    }

    @Test
    public void decryptsThePassword() {
        command.execute(context, request);
        verify(cryptography, times(1)).decrypt(request.encryptedPassword);
    }

    @Test
    public void kicksOffSubCommandToChangePassword() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("SetPassword-" + username), any(Function.class), eq(SysAdminAction.class));
    }

    @Test
    public void forIspconfigSubCommandCallsHfsServiceToChangePassword() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        verify(sysAdminService, times(1)).changePassword(hfsVmId, username, dummyPassword, controlPanel);
        assertEquals(dummyHfsAction, action);
    }

    @Test
    public void forCpanelSubCommandCallsHfsServiceToChangePassword() {
        controlPanel = Image.ControlPanel.CPANEL.toString().toLowerCase();
        request.controlPanel = controlPanel;
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        verify(sysAdminService, times(1)).changePassword(hfsVmId, username, dummyPassword, "cpanel");
        assertEquals(dummyHfsAction, action);
    }

    @Test
    public void forPleskSubCommandCallsHfsServiceToChangePassword() {
        controlPanel = Image.ControlPanel.PLESK.toString().toLowerCase();
        request.controlPanel = controlPanel;
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        verify(sysAdminService, times(1)).changePassword(hfsVmId, username, dummyPassword, "plesk");
        assertEquals(dummyHfsAction, action);
    }

    @Test
    public void forNonControlPanelImageSubCommandCallsHfsServiceToChangePassword() {
        controlPanel = null;
        request.controlPanel = controlPanel;
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        verify(sysAdminService, times(1)).changePassword(hfsVmId, username, dummyPassword, null);
        assertEquals(dummyHfsAction, action);
    }

    @Test
    public void waitsForChangePasswordActionCompletion() {
        command.execute(context, request);
        verify(context, times(1)).execute("WaitForSet-"+username, WaitForSysAdminAction.class, dummyHfsAction);
    }
}