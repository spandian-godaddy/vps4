package com.godaddy.vps4.sysadmin;

import com.google.inject.AbstractModule;

public class SysAdminModule extends AbstractModule {

    @Override
    public void configure() {
        bind(VmUsageService.class);
    }
}
