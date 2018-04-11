package com.godaddy.vps4.phase2;

import java.util.UUID;

import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.google.inject.AbstractModule;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class Phase2ExternalsModule extends AbstractModule {
    private final static CreditService creditService = Mockito.mock(CreditService.class);
    private final static VmService vmService = Mockito.mock(VmService.class);

    @Override
    public void configure() {
        mockVmCredit(AccountStatus.ACTIVE);
        bind(CreditService.class).toInstance(creditService);

        // HFS services
        mockHfsVm("ACTIVE");
        bind(VmService.class).toInstance(vmService);

        CommandService commandService = Mockito.mock(CommandService.class);
        CommandState commandState = new CommandState();
        commandState.commandId = UUID.randomUUID();
        Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                .thenReturn(commandState);
        bind(CommandService.class).toInstance(commandService);

    }

    public static void mockVmCredit(AccountStatus accountStatus) {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachineCredit credit = new VirtualMachineCredit(orionGuid, 10, 0, 1, "linux", "myh", null, GDUserMock.DEFAULT_SHOPPER,
                accountStatus, null, null, false, "1");
        Mockito.when(creditService.getVirtualMachineCredit(Mockito.any())).thenReturn(credit);
    }

    public static void mockHfsVm(String status) {
        Vm hfsVm = new Vm();
        hfsVm.status = status;
        hfsVm.vmId = 98765;
        Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);
    }



}
