package com.godaddy.vps4.orchestration.hfs.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.Image;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.ChangePasswordRequestBody;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;


public class SetPasswordTest {
    private final SysAdminService sysAdminService = mock(SysAdminService.class);
    private final Cryptography cryptography = mock(Cryptography.class);
    private final String dummyPassword = "foobar";
    private final String username = "jdoe";
    private final SysAdminAction dummyHfsAction = mock(SysAdminAction.class);

    private SetPassword command;
    private String controlPanel = Image.ISPCONFIG;
    private CommandContext context;
    private SetPassword.Request request;
    long hfsVmId = 123L;

    @Captor private ArgumentCaptor<Function<CommandContext, SysAdminAction>> changePasswordLambdaCaptor;
    @Captor private ArgumentCaptor<ChangePasswordRequestBody> changePasswordSysadminCaptor;

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
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
        when(sysAdminService.changePassword(eq(0L), eq(null), eq(null), eq(null), any(ChangePasswordRequestBody.class)))
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
    public void forIspConfigSubCommandCallsHfsServiceToChangePassword() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        assertEquals(dummyHfsAction, action);

        verify(sysAdminService, times(1))
                .changePassword(eq(0L), eq(null), eq(null), eq(null), changePasswordSysadminCaptor.capture());
        assertEquals(hfsVmId, changePasswordSysadminCaptor.getValue().serverId);
        assertEquals(username, changePasswordSysadminCaptor.getValue().username);
        assertEquals(dummyPassword, changePasswordSysadminCaptor.getValue().password);
        assertEquals(controlPanel, changePasswordSysadminCaptor.getValue().controlPanel);
    }

    @Test
    public void forCpanelSubCommandCallsHfsServiceToChangePassword() {
        controlPanel = Image.ControlPanel.CPANEL.toString().toLowerCase();
        request.controlPanel = controlPanel;
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        assertEquals(dummyHfsAction, action);

        verify(sysAdminService, times(1))
                .changePassword(eq(0L), eq(null), eq(null), eq(null), changePasswordSysadminCaptor.capture());
        assertEquals(hfsVmId, changePasswordSysadminCaptor.getValue().serverId);
        assertEquals(username, changePasswordSysadminCaptor.getValue().username);
        assertEquals(dummyPassword, changePasswordSysadminCaptor.getValue().password);
        assertEquals(controlPanel, changePasswordSysadminCaptor.getValue().controlPanel);
    }

    @Test
    public void forPleskSubCommandCallsHfsServiceToChangePassword() {
        controlPanel = Image.ControlPanel.PLESK.toString().toLowerCase();
        request.controlPanel = controlPanel;
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        assertEquals(dummyHfsAction, action);

        verify(sysAdminService, times(1))
                .changePassword(eq(0L), eq(null), eq(null), eq(null), changePasswordSysadminCaptor.capture());
        assertEquals(hfsVmId, changePasswordSysadminCaptor.getValue().serverId);
        assertEquals(username, changePasswordSysadminCaptor.getValue().username);
        assertEquals(dummyPassword, changePasswordSysadminCaptor.getValue().password);
        assertEquals(controlPanel, changePasswordSysadminCaptor.getValue().controlPanel);
    }

    @Test
    public void forNonControlPanelImageSubCommandCallsHfsServiceToChangePassword() {
        controlPanel = null;
        request.controlPanel = controlPanel;
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("SetPassword-" + username), changePasswordLambdaCaptor.capture(), eq(SysAdminAction.class));

        Function<CommandContext, SysAdminAction> lambda = changePasswordLambdaCaptor.getValue();
        SysAdminAction action = lambda.apply(context);
        assertEquals(dummyHfsAction, action);

        verify(sysAdminService, times(1))
                .changePassword(eq(0L), eq(null), eq(null), eq(null), changePasswordSysadminCaptor.capture());
        assertEquals(hfsVmId, changePasswordSysadminCaptor.getValue().serverId);
        assertEquals(username, changePasswordSysadminCaptor.getValue().username);
        assertEquals(dummyPassword, changePasswordSysadminCaptor.getValue().password);
        assertEquals(controlPanel, changePasswordSysadminCaptor.getValue().controlPanel);
    }

    @Test
    public void waitsForChangePasswordActionCompletion() {
        command.execute(context, request);
        verify(context, times(1)).execute("WaitForSet-"+username, WaitForSysAdminAction.class, dummyHfsAction);
    }
}