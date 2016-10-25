package com.godaddy.vps4.web;

import com.godaddy.vps4.web.validator.ValidatorResource;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {

        bind(VmResource.class);
        bind(VmPatchResource.class);
        bind(ValidatorResource.class);

        bind(TestResource.class);
    }
}
