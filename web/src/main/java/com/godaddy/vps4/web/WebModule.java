package com.godaddy.vps4.web;

import com.godaddy.vps4.web.cache.CacheResource;
import com.godaddy.vps4.web.controlPanel.cpanel.CPanelResource;
import com.godaddy.vps4.web.controlPanel.plesk.PleskResource;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource;
import com.godaddy.vps4.web.monitoring.VmMonitoringResource;
import com.godaddy.vps4.web.security.AdminAuthFilter;
import com.godaddy.vps4.web.support.SupportResource;
import com.godaddy.vps4.web.support.UserSupportResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.sysadmin.UsageStatsResource;
import com.godaddy.vps4.web.util.AlphaHelper;
import com.godaddy.vps4.web.validator.ValidatorResource;
import com.godaddy.vps4.web.vm.ImageResource;
import com.godaddy.vps4.web.vm.UserResource;
import com.godaddy.vps4.web.vm.VmActionResource;
import com.godaddy.vps4.web.vm.VmFlavorResource;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    public void configure() {
        bind(StatusResource.class);

        bind(VmResource.class);
        bind(VmActionResource.class);
        bind(VmFlavorResource.class);
        bind(VmPatchResource.class);
        bind(VmMailRelayResource.class);
        bind(VmMonitoringResource.class);
        bind(ValidatorResource.class);
        bind(CPanelResource.class);
        bind(PleskResource.class);
        bind(SysAdminResource.class);
        bind(UserResource.class);
        bind(CreditResource.class);
        bind(VmActionResource.class);
        bind(ImageResource.class);
        bind(UsageStatsResource.class);
        bind(CacheResource.class);

        bind(SupportResource.class);
        bind(UserSupportResource.class);

        bind(Vps4ExceptionMapper.class);
        bind(AdminAuthFilter.class);
        bind(AlphaHelper.class);
    }
}

