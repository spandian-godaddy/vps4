package com.godaddy.vps4.phase2;

import com.godaddy.vps4.vm.ActionType;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class CancelActionModule extends AbstractModule {
    @Override
    public void configure() {
        MapBinder<ActionType, String> actionTypeToCancelCmdNameMapBinder
                = MapBinder.newMapBinder(binder(), ActionType.class, String.class);
    }

}
