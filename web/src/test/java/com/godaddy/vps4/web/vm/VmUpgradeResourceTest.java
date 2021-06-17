package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertNull;
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
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmUpgradeResource.UpgradeVmRequest;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmUpgradeResourceTest {

    private GDUser user;
    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private ActionService actionService;
    private VmUpgradeResource resource;
    private VirtualMachineCredit testCredit;
    private VirtualMachine testVm;
    private CommandService commandService;
    private Cryptography cryptography;
    private Config config;


    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();

        testCredit = setupCredit(Boolean.TRUE);

        testVm = new VirtualMachine();
        testVm.validOn = Instant.now();
        testVm.canceled = Instant.MAX;
        testVm.validUntil = Instant.MAX;
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = testCredit.getOrionGuid();

        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        actionService = mock(ActionService.class);
        commandService = mock(CommandService.class);
        cryptography = mock(Cryptography.class);
        config = mock(Config.class);

        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);

        when(actionService.getActionList(any())).thenReturn(null);

        Action testAction = new Action(123L, testVm.vmId, ActionType.UPGRADE_VM, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);
        when(actionService.getAction(anyLong())).thenReturn(testAction);

        resource = new VmUpgradeResource(user, virtualMachineService, creditService, actionService, commandService,
                                         cryptography, config);
    }

    private VirtualMachineCredit setupCredit(Boolean planChangePending) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(40));

        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("plan_change_pending", planChangePending.toString());

        return new VirtualMachineCredit.Builder(mock(DataCenterService.class))
            .withAccountGuid(UUID.randomUUID().toString())
            .withAccountStatus(AccountStatus.ACTIVE)
            .withShopperID(user.getShopperId())
            .withProductMeta(productMeta)
            .withPlanFeatures(planFeatures)
            .build();
    }

    @Test(expected = Vps4Exception.class)
    public void testUpgradeVmNoPlanChangePending() {
        testCredit = setupCredit(Boolean.FALSE);
        when(creditService.getVirtualMachineCredit(testCredit.getOrionGuid())).thenReturn(testCredit);
        UpgradeVmRequest upgradeVmRequest= new UpgradeVmRequest();
        resource.upgradeVm(testVm.vmId, upgradeVmRequest);
    }

    @Test
    public void testUpgradeOpenstackVmWithValidPassword() {
        testVm.spec = new ServerSpec();
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        testVm.image = new Image();
        testVm.image.controlPanel = Image.ControlPanel.MYH;
        when(creditService.getVirtualMachineCredit(testCredit.getOrionGuid())).thenReturn(testCredit);
        String password = "T0ta!1yRand0m";
        UpgradeVmRequest upgradeVmRequest= new UpgradeVmRequest();
        upgradeVmRequest.password = password;
        resource.upgradeVm(testVm.vmId, upgradeVmRequest);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.UPGRADE_VM), anyString(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testUpgradeOpenstackVmWithInvalidPassword() {
        testVm.spec = new ServerSpec();
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        testVm.image = new Image();
        testVm.image.controlPanel = Image.ControlPanel.MYH;
        when(creditService.getVirtualMachineCredit(testCredit.getOrionGuid())).thenReturn(testCredit);
        String password = "";
        UpgradeVmRequest upgradeVmRequest= new UpgradeVmRequest();
        upgradeVmRequest.password = password;
        resource.upgradeVm(testVm.vmId, upgradeVmRequest);
    }

    @Test
    public void testUpgradeOHVmNoPasswordNeeded() {
        testVm.spec = new ServerSpec();
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        when(creditService.getVirtualMachineCredit(testCredit.getOrionGuid())).thenReturn(testCredit);
        UpgradeVmRequest upgradeVmRequest= new UpgradeVmRequest();
        resource.upgradeVm(testVm.vmId, upgradeVmRequest);
        assertNull(upgradeVmRequest.password);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.UPGRADE_VM), anyString(), anyString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpgradeDedicatedNotSupported() {
        testVm.spec = new ServerSpec();
        testVm.spec.serverType = new ServerType();
        testVm.spec.serverType.platform = ServerType.Platform.OVH;
        when(creditService.getVirtualMachineCredit(testCredit.getOrionGuid())).thenReturn(testCredit);
        UpgradeVmRequest upgradeVmRequest= new UpgradeVmRequest();
        resource.upgradeVm(testVm.vmId, upgradeVmRequest);
    }


}
