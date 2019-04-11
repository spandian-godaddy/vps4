package com.godaddy.vps4.phase2;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.DataCenterService;
import gdg.hfs.vhfs.ecomm.Account;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.google.inject.AbstractModule;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;

import static org.mockito.Mockito.mock;

public class Phase2ExternalsModule extends AbstractModule {
    private final static CreditService creditService = mock(CreditService.class);
    private final static VmService vmService = mock(VmService.class);

    @Override
    public void configure() {
        mockVmCredit(AccountStatus.ACTIVE);
        bind(CreditService.class).toInstance(creditService);

        // HFS services
        mockHfsVm("ACTIVE");
        bind(VmService.class).toInstance(vmService);

        CommandService commandService = mock(CommandService.class);
        CommandState commandState = new CommandState();
        commandState.commandId = UUID.randomUUID();
        Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                .thenReturn(commandState);
        bind(CommandService.class).toInstance(commandService);

    }

    public static void mockVmCredit(AccountStatus accountStatus) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(10));
        planFeatures.put("managed_level", String.valueOf(0));
        planFeatures.put("control_panel_type", String.valueOf("myh"));
        planFeatures.put("operatingsystem", String.valueOf("linux"));
        planFeatures.put("monitoring", String.valueOf(1));

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(UUID.randomUUID().toString())
                .withAccountStatus(Account.Status.valueOf(accountStatus.toString().toLowerCase()))
                .withShopperID(GDUserMock.DEFAULT_SHOPPER)
                .withPlanFeatures(planFeatures)
                .build();

        Mockito.when(creditService.getVirtualMachineCredit(Mockito.any())).thenReturn(credit);
    }

    public static void mockHfsVm(String status) {
        Vm hfsVm = new Vm();
        hfsVm.status = status;
        hfsVm.vmId = 98765;
        Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);
    }



}
