package com.godaddy.vps4.console;

import com.google.inject.AbstractModule;

public class ConsoleModule extends AbstractModule{

    @Override
    public void configure() {
        bind(ConsoleService.class).to(SpiceConsole.class);
    }
}