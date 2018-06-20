package com.godaddy.vps4.web;

import com.godaddy.vps4.orchestration.vm.Vps4CancelAction;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandDescriptor;

public class ActionCancelModule extends AbstractModule {
    @Override
    protected void configure() {
        setupActionCancelBindings();
    }

    private void setupActionCancelBindings() {
        MapBinder<ActionType, String> actionTypeToCancelCmdNameMapBinder
                = MapBinder.newMapBinder(binder(), ActionType.class, String.class);
        actionTypeToCancelCmdNameMapBinder.addBinding(ActionType.SET_HOSTNAME)
                .toInstance(getCommandName(Vps4CancelAction.class));
//        actionTypeToCancelCmdNameMapBinder.addBinding(ActionType.ADD_SUPPORT_USER)
//            .toInstance(getCommandName(Vps4CancelAction.class));
    }

    private String getCommandName(Class<? extends Command> cls) {
        return CommandDescriptor.fromClass(cls).getCommandName();
    }
}
