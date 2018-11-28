package com.godaddy.vps4.phase2;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import org.mockito.Mockito;

import java.util.UUID;

public class CancelActionModule extends AbstractModule {
    @Override
    public void configure() {
        MapBinder<ActionType, String> actionTypeToCancelCmdNameMapBinder
                = MapBinder.newMapBinder(binder(), ActionType.class, String.class);
    }

}
