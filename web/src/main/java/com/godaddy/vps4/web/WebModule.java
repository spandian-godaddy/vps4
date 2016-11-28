package com.godaddy.vps4.web;

import com.godaddy.vps4.web.validator.ValidatorResource;
import com.godaddy.vps4.web.vm.UserResource;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.cpanel.CPanelResource;
import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {
        bind(StatusResource.class);

        bind(VmResource.class);
        bind(VmPatchResource.class);
        bind(ValidatorResource.class);
        bind(CPanelResource.class);
        bind(SysAdminResource.class);
        bind(UserResource.class);
    }
}
