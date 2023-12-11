package com.godaddy.vps4.productPackage;

import com.godaddy.vps4.productPackage.jdbc.JdbcPackageService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PackageModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PackageService.class).to(JdbcPackageService.class).in(Scopes.SINGLETON);
    }
}
