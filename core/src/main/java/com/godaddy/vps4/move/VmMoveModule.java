package com.godaddy.vps4.move;

import com.godaddy.vps4.move.jdbc.JdbcVmMoveImageMapService;
import com.godaddy.vps4.move.jdbc.JdbcVmMoveSpecMapService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class VmMoveModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(VmMoveImageMapService.class).to(JdbcVmMoveImageMapService.class).in(Scopes.SINGLETON);
        bind(VmMoveSpecMapService.class).to(JdbcVmMoveSpecMapService.class).in(Scopes.SINGLETON);
    }
}
