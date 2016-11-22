package com.godaddy.vps4.security;

import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.google.inject.AbstractModule;

public class SecurityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ProjectService.class).to(JdbcProjectService.class);
        bind(Vps4UserService.class).to(JdbcVps4UserService.class);
    }
}