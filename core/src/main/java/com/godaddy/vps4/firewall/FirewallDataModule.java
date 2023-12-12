package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.jdbc.JdbcFirewallDataService;
import com.google.inject.AbstractModule;

public class FirewallDataModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FirewallDataService.class).to(JdbcFirewallDataService.class);
    }
}