package com.godaddy.vps4.web;

import com.godaddy.vps4.web.validator.ValidatorResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {

        bind(VmResource.class);
        bind(ValidatorResource.class);
    }
}
