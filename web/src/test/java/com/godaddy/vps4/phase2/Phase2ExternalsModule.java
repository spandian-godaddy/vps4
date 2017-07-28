package com.godaddy.vps4.phase2;

import com.godaddy.vps4.credit.CreditService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.VmService;
import org.mockito.Mockito;

import java.util.UUID;

public class Phase2ExternalsModule extends AbstractModule {
    private final CommandService commandService;

    {
        commandService = Mockito.mock(CommandService.class);
        CommandState commandState = new CommandState();
        commandState.commandId = UUID.randomUUID();
        Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                .thenReturn(commandState);

    }

    @Override
    public void configure() {
    }

    @Provides
    public CreditService provideMockCreditService() {
        return Mockito.mock(CreditService.class);
    }

    @Provides
    public CommandService provideMockCommandService() {
        return commandService;
    }

    @Provides
    public VmService provideMockHFSVmService() {
        return Mockito.mock(VmService.class);
    }
}

