package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.jdbc.JdbcFirewallService;
import com.google.inject.AbstractModule;

public class FirewallsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FirewallService.class).to(JdbcFirewallService.class);
    }
}