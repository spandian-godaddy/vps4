package com.godaddy.vps4.web.mailrelay;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.mailrelay.Vps4SetMailRelayQuota;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource.MailRelayQuotaPatch;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.util.UUID;


public class VmMailRelayResourceTest {
    private GDUser user;
    private long actionId = 123;
    private Action mailRelayAction = mock(Action.class);
    private GDUser patchUser = mock(GDUser.class);
    private ActionService actionService = mock(ActionService.class);
    private CreditService creditService = mock(CreditService.class);
    private NetworkService networkService = mock(NetworkService.class);
    private CommandService commandService = mock(CommandService.class);
    private CommandState commandState = mock(CommandState.class);
    private VmResource vmResource = mock(VmResource.class);
    private IpAddress ipAddress = mock(IpAddress.class);
    private VirtualMachineCredit vmCredit = mock(VirtualMachineCredit.class);
    private VirtualMachine testVm;
    private MailRelayQuotaPatch mailRelayQuotaPatch;

    @Inject DataSource dataSource;
    @Inject Vps4UserService userService;

    @Captor private ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor;

    private Injector injector = Guice.createInjector(
        new DatabaseModule(),
        new SecurityModule(),
        new AbstractModule() {
            @Override
            public void configure() {
                bind(VmResource.class).toInstance(vmResource);
                bind(NetworkService.class).toInstance(networkService);
                bind(CommandService.class).toInstance(commandService);
                bind(CreditService.class).toInstance(creditService);
                bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
                bind(VirtualMachineService.class).toInstance(mock(VirtualMachineService.class));
                bind(ActionService.class).toInstance(actionService);
            }

            @Provides
            public GDUser provideUser() {
                return patchUser;
            }
        }
    );

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);

        user = GDUserMock.createShopper();
        createTestVM(user.getShopperId());

        commandState.commandId = UUID.randomUUID();
        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
        when(vmResource.getVm(any(UUID.class))).thenReturn(testVm);

        ipAddress.ipAddress = "1.1.1.1";
        when(networkService.getVmPrimaryAddress(eq(testVm.vmId))).thenReturn(ipAddress);

        when(creditService.getVirtualMachineCredit(eq(testVm.orionGuid))).thenReturn(vmCredit);

        when(patchUser.role()).thenReturn(GDUser.Role.HS_AGENT);

        when(actionService.createAction(eq(testVm.vmId), eq(ActionType.UPDATE_MAILRELAY_QUOTA), any(), any()))
            .thenReturn(actionId);
        when(actionService.getAction(eq(actionId))).thenReturn(mailRelayAction);

        mailRelayQuotaPatch = new MailRelayQuotaPatch();
        mailRelayQuotaPatch.quota = 1000;
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmMailRelayResource getVmMailRelayResource() {
        return injector.getInstance(VmMailRelayResource.class);
    }

    private void createTestVM(String shopperId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(shopperId, "1");
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
    }

    @Test
    public void getsVmDetails() {
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
        verify(vmResource, times(1)).getVm(eq(testVm.vmId));
    }

    @Test
    public void getsVmCreditDetails() {
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
        verify(creditService, times(1)).getVirtualMachineCredit(eq(testVm.orionGuid));
    }

    @Test(expected = Vps4Exception.class)
    public void hsAgentGoDaddyCustomerQuotaOverLimitNotAllowed() {
        vmCredit.resellerId = "1";
        mailRelayQuotaPatch.quota = 10001;
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
    }

    @Test(expected = Vps4Exception.class)
    public void hsAgentBrandResellerCustomerQuotaOverLimitNotAllowed() {
        vmCredit.resellerId = "525848";
        mailRelayQuotaPatch.quota = 25001;
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
    }

    @Test
    public void adminHasNoLimitsCheckForGodaddyCustomer() {
        when(patchUser.role()).thenReturn(GDUser.Role.ADMIN);
        vmCredit.resellerId = "1";
        mailRelayQuotaPatch.quota = 10001;
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
    }

    @Test
    public void adminHasNoLimitsCheckForBrandResellerCustomer() {
        when(patchUser.role()).thenReturn(GDUser.Role.ADMIN);
        vmCredit.resellerId = "525848";
        mailRelayQuotaPatch.quota = 25001;
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
    }

    @Test
    public void hsTechLeadHasNoLimitsCheckForGodaddyCustomer() {
        when(patchUser.role()).thenReturn(GDUser.Role.HS_LEAD);
        vmCredit.resellerId = "1";
        mailRelayQuotaPatch.quota = 10001;
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
    }

    @Test
    public void hsTechLeadHasNoLimitsCheckForBrandResellerCustomer() {
        when(patchUser.role()).thenReturn(GDUser.Role.HS_LEAD);
        vmCredit.resellerId = "525848";
        mailRelayQuotaPatch.quota = 25001;
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
    }

    @Test
    public void getsPrimaryIpAddress() {
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
        verify(creditService, times(1)).getVirtualMachineCredit(eq(testVm.orionGuid));
    }

    @Test
    public void createsNewActionToTrackMailRelayUpdate() {
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
        verify(actionService, times(1))
            .createAction(eq(testVm.vmId), eq(ActionType.UPDATE_MAILRELAY_QUOTA), any(), any());
    }

    @Test
    public void testQueuesCommandToProcessUpdate() {
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);

        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());

        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        Vps4SetMailRelayQuota.Request request = (Vps4SetMailRelayQuota.Request) commandSpec.request;

        Assert.assertEquals("Vps4SetMailRelayQuota", commandSpec.command);
        Assert.assertEquals(actionId, request.actionId);
        Assert.assertEquals(1000, request.mailRelayQuota);
    }

    @Test
    public void returnsNewlyCreatedAction() {
        VmMailRelayResource mailRelayResource = getVmMailRelayResource();
        Action retAction = mailRelayResource.updateMailRelayQuota(testVm.vmId, mailRelayQuotaPatch);
        Assert.assertEquals(mailRelayAction, retAction);
    }
}