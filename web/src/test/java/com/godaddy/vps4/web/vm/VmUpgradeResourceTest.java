package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.web.Vps4Exception;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmUpgradeResourceTest {

    private GDUser user;
    private CreditService creditService;
    private ActionService actionService;
    private VmUpgradeResource resource;
    private VirtualMachineCredit testCredit;
    private VirtualMachine testVm;


    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();

        testCredit = setupCredit();

        testVm = new VirtualMachine();
        testVm.validOn = Instant.now();
        testVm.canceled = Instant.MAX;
        testVm.validUntil = Instant.MAX;
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = testCredit.getEntitlementId();
        testVm.dataCenter = new DataCenter(1, "phx3");

        VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        actionService = mock(ActionService.class);
        CommandService commandService = mock(CommandService.class);
        Cryptography cryptography = mock(Cryptography.class);
        Config config = mock(Config.class);

        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);

        when(actionService.getActionList(any())).thenReturn(null);

        Action testAction = new Action(123L, testVm.vmId, ActionType.UPGRADE_VM, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);
        when(actionService.getAction(anyLong())).thenReturn(testAction);

        resource = new VmUpgradeResource(user, virtualMachineService, creditService, actionService, commandService,
                cryptography, config);
    }

    private VirtualMachineCredit setupCredit() {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(40));

        return new VirtualMachineCredit.Builder()
            .withAccountGuid(UUID.randomUUID().toString())
            .withAccountStatus(AccountStatus.ACTIVE)
            .withShopperID(user.getShopperId())
            .withPlanFeatures(planFeatures)
            .build();
    }

    @Test
    public void testUpgradeOHVm() {
        testVm.spec = new ServerSpec();
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        when(creditService.getVirtualMachineCredit(testCredit.getEntitlementId())).thenReturn(testCredit);
        resource.upgradeVm(testVm.vmId);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.UPGRADE_VM), anyString(), anyString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpgradeDedicatedNotSupported() {
        testVm.spec = new ServerSpec();
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OVH;
        when(creditService.getVirtualMachineCredit(testCredit.getEntitlementId())).thenReturn(testCredit);
        resource.upgradeVm(testVm.vmId);
    }

    @Test(expected = Vps4Exception.class)
    public void testUpgradeNotAllowed() {
        testVm.spec = new ServerSpec();
        testVm.spec.tier = 40;
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        when(creditService.getVirtualMachineCredit(testCredit.getEntitlementId())).thenReturn(testCredit);
        resource.upgradeVm(testVm.vmId);
    }

}
